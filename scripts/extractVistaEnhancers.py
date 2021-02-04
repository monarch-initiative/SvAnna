import requests
from typing import List
import os
import stat


class Enhancer:
    """
    Simple class to store positive H. sapiens enhancers
    """
    def __init__(self, coords: str, name: str, tissues: List) -> None:
        fields = coords.split(":")
        if len(fields) != 2:
            raise ValueError("Malformed coordinates (chrom): ", self._coords)
        self._chrom = fields[0]
        fields = fields[1].split("-")
        if len(fields) != 2:
            raise ValueError("Malformed coordinates (pos): ", self._coords)
        self._begin = int(fields[0]) # use int transformation to check format
        self._end = int(fields[1])
        self._name = name
        self._tissues = tissues


    def get_bed4_line(self):
        """
        Create a line for using liftover, e.g., chr1\t123\t345\tNAME
        We will use the NAME field to include all of the information about tissues
        Example output line: chr7    21003280        21004750        element 110(forebrain[4/4])
        """
        name = self._name.replace(" ","_") # Note we need to replace space to keep liftover from turning it into tab
        NAME = "%s(%s)" % (name, ";".join(self._tissues))
        return "%s\t%d\t%d\t%s" % (self._chrom, self._begin, self._end, NAME)

    @property
    def chrom(self):
        return self._chrom

    @property
    def begin(self):
        return self._begin

    @property
    def end(self):
        return self._end

    @property
    def name(self):
        self._name

    def get_coords(self):
        return "%s:%d-%d" % (self._chrom, self._begin, self._end)


    def __str__(self):
        return self._name + ", " + self.get_coords() + ", " + ";".join(self._tissues)


def download_vista_if_needed() -> str:
    vista_url = 'https://enhancer.lbl.gov/cgi-bin/imagedb3.pl?page_size=100;show=1;search.result=yes;page=1;form=search;search.form=no;action=search;search.sequence=1'
    local_filename = 'vista-hg19-mm9.txt'
    if os.path.exists(local_filename):
        print("[INFO] Not re-downloading VISTA data. To download a new copy, delete %s" % local_filename)
    r = requests.get(vista_url, allow_redirects=True)
    open(local_filename, 'wb').write(r.content)
    return local_filename

def download_liftover_if_needed():
    liftover_url = 'http://hgdownload.cse.ucsc.edu/admin/exe/linux.x86_64/liftOver'
    local_filename = 'liftOver'
    if os.path.exists(local_filename):
        print("[INFO] Not re-downloading liftOver. To download a new copy, delete %s" % local_filename)
    r = requests.get(liftover_url, allow_redirects=True)
    open(local_filename, 'wb').write(r.content)
    st = os.stat(local_filename)
    os.chmod(local_filename, st.st_mode | stat.S_IEXEC)
    return local_filename

def download_liftover_chainfile_if_needed():
    chain_url = 'http://hgdownload.cse.ucsc.edu/goldenPath/hg19/liftOver/hg19ToHg38.over.chain.gz'
    local_filename = 'hg19ToHg38.over.chain.gz'
    if os.path.exists(local_filename):
        print("[INFO] Not re-downloading chain file. To download a new copy, delete %s" % local_filename)
    r = requests.get(chain_url, allow_redirects=True)
    open(local_filename, 'wb').write(r.content)
    return local_filename

def extract_human_positive_enhancer_coordinates(local_filename: str):
    n_pos = 0
    n_neg = 0
    positive_enhancers = []
    with open(local_filename) as f:
        for line in f:
            if line.startswith("<pre>"):
                line = line[5:]
            if line.startswith(">Human"):
                fields = line.rstrip().split('|')
                if len(fields) < 4:
                    raise ValueError("Malformed header line: ", line)
                coords = fields[1].strip()
                name = fields[2].strip().replace('\t', ' ')
                if '\t' in name:
                    raise ValueError(" t in ame")
                status = fields[3].strip()
                if status == 'positive':
                    n_pos += 1
                elif status == 'negative':
                    n_neg += 1
                    continue
                if len(fields) < 5:
                    raise ValueError("Malformed positive line -- expected at least one tissue. ", line)
                tmp = [ t.strip() for t in fields[4:] ] # remove white space
                tissues = []
                # Transform hindbrain (rhombencephalon) to hindbrain_rhombencephalon (for example)
                for t in tmp:
                    t = t.replace(' ', '')
                    t = t.replace('(', '_')
                    t = t.replace(')', '')
                    tissues.append(t)
                
                e = Enhancer(coords=coords, name=name, tissues=tissues)
                positive_enhancers.append(e)
    print("[INFO] Positive human examples %d, negative %d" % (n_pos, n_neg))
    return positive_enhancers


def write_bed4(hg19_enhancers: List, fname: str = "vista-hg19.bed") -> str:
    if os.path.exists(fname):
        print("[INFO] Not re-writing existing BED4 file. Delete %s to write a new one" % fname)
        return fname
    fh = open(fname, 'wt')
    for e in hg19_enhancers:
        bed4_line = e.get_bed4_line()
        fh.write(bed4_line + "\n")
    fh.close()
    return fname


def write_statistics(hg38enhancers: str, unlifted:str):
    n_unlifted = 0
    with open(unlifted) as f:
        for line in f:
            print("[WARN] Could not lift-over: ", line)
            n_unlifted
    if n_unlifted == 0:
        print("[INFO] We were able to liftover all enhancers")
    else:
        print("[WARN] We could not liftover %d enhancers" % n_unlifted)
    tissueset = set()
    n_lifted = 0
    with open(hg38enhancers) as f:
        for line in f:
            n_lifted += 1
            fields = line.rstrip().split("\t")
            if len(fields) != 4:
                raise ValueError("Bad line in bed4: ", line)
            tissues = fields[3]
            i = tissues.find("(")
            if i < 0:
                raise ValueError("Malformed tissue field ", tissues)
            tissues = tissues[i+1:].replace(")","")
            tissue_types = tissues.split(";")
            for t in tissue_types:
                i = t.find("[")
                t = t[0:i]
                tissueset.add(t)
    print("[INFO] We lifted over %d enhancers" % n_lifted)
    print("[INFO] We found %d tissues" % len(tissueset))
    for t in tissueset:
        print("\t%s" % t)


class Uberon:
    def __init__(self, id:str, label:str) -> None:
        self._id = id
        self._label = label

    @property
    def id(self):
        return self._id
    
    @property
    def label(self):
        return self._label

    def __str__(self):
        return "%s[%s]" % (self._label, self._id)

    def get_str(self):
        return self.__str__

def initialize_uberon_map():
    """
     this map assigns UBERON terms to the strings used in VISTA
    """
    uberon_map = {'branchialarch': Uberon(id='UBERON:0002539', label='pharyngeal arch'),
    'tail': Uberon(id='UBERON:0002415', label='tail'),
    'melanocytes' :Uberon(id='CL:0000148', label='melanocyte'),
    'neuraltube': Uberon(id='UBERON:0001049',label='neural tube'),
    'heart': Uberon(id='UBERON:0000948', label='heart'),
    'limb': Uberon(id='UBERON:0002101', label='limb'),
    'dorsalrootganglion': Uberon(id='UBERON:0000044', label='dorsal root ganglion'),
    'cranialnerve': Uberon(id='UBERON:0001785', label='cranial nerve'),
    'trigeminalV_ganglion,cranial': Uberon(id='UBERON:0001675', label='trigeminal ganglion'),
    'liver': Uberon(id='UBERON:0002107', label='liver'),
    'somite': Uberon(id='UBERON:0002329', label='somite'),
    'nose': Uberon(id='UBERON:0000004', label='nose'),
    'facialmesenchyme': Uberon(id='UBERON:0009891', label='facial mesenchyme'),
    'ear': Uberon(id='UBERON:0001690', label='ear'),
    'hindbrain_rhombencephalon': Uberon(id='UBERON:0007277',label='presumptive hindbrain'),
    'pancreas': Uberon(id='UBERON:0001264', label='pancreas'),
    'forebrain': Uberon(id='UBERON:0001890', label='forebrain'),
    'bloodvessels': Uberon(id='UBERON:0001981', label='blood vessel'),
    'eye': Uberon(id='UBERON:0000970', label='eye'),
    'genitaltubercle': Uberon(id='UBERON:0011757', label='differentiated genital tubercle'),
    'midbrain_mesencephalon': Uberon(id='UBERON:0009616', label='presumptive midbrain'),
    'other': Uberon(id='UBERON:0001062', label='anatomical entity') # best we can do, but potentially valuable since developmental
    }
    return uberon_map


###################
###################

vista_download_file = download_vista_if_needed()
hg19_enhancers = extract_human_positive_enhancer_coordinates(vista_download_file)
print("[INFO] Extracted %d positive hg19 enhancers" % len(hg19_enhancers))
print("[INFO] Examples:")
## Show a few examples
for e in hg19_enhancers[0:5]:
    print("\t%s" % e)

download_liftover_if_needed()
chainfile = download_liftover_chainfile_if_needed()
print("[INFO] Will use %s to transform VISTA to hg38" % chainfile)
bed4_file = write_bed4(hg19_enhancers)

## NOW WE DO THE LIFTOVER
output_BED4 = "vista-hg38.bed"
liftover_command = "./liftOver %s hg19ToHg38.over.chain.gz %s unlifted.bed" % (bed4_file, output_BED4)
result = os.system(liftover_command)
if result != 0:
    print("[ERROR] Could not execute liftover: ", liftover_command)
write_statistics(hg38enhancers= output_BED4, unlifted='unlifted.bed')

## NOW WE OUTPUT THE RESULTS FOR USE IN THE JAVA PROGRAM
uberon_map = initialize_uberon_map()
fh = open("hg38-vista-enhancers.tsv", 'wt')
# header line
header = ['name', 'chr', 'begin', 'end', 'tissues']
fh.write("\t".join(header) + "\n")
with open(output_BED4) as f:
    for line in f:
        fields = line.rstrip().split("\t")
        if len(fields) != 4:
            raise ValueError("Bad line in bed4: ", line)
        chrom = fields[0]
        begin = fields[1]
        end = fields[2]
        tissues = fields[3]
        i = tissues.find("(")
        name = tissues[0:i].replace("_", " ")
        if i < 0:
            raise ValueError("Malformed tissue field ", tissues)
        tissues = tissues[i+1:].replace(")","")
        tissue_types = tissues.split(";")
        uberon_list = []
        for t in tissue_types:
            i = t.find("[")
            t = t[0:i]
            if not t in uberon_map:
                raise ValueError("Could not find ")
            uberon = uberon_map.get(t)
            uberterm = "%s[%s]" % (uberon.label, uberon.id)
            print(uberterm)
            uberon_list.append(uberterm)
        fields = [name, chrom, begin, end, ";".join(uberon_list)]
        fh.write("\t".join(fields) + "\n")
    fh.close()
        
