package org.jax.svanna.db.annotation;

import org.jax.svanna.core.reference.TadBoundary;
import org.jax.svanna.db.IngestDao;
import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.Strand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class TadBoundaryDao implements IngestDao<TadBoundary> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TadBoundaryDao.class);

    private final DataSource dataSource;

    public TadBoundaryDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public int insertItem(TadBoundary item) {
        int updated = 0;

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            String sql = "insert into SVANNA.TAD_BOUNDARY(CONTIG, START, END, ID, STABILITY) " +
                    "VALUES ( ?, ?, ?, ?, ?)";
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setInt(1, item.contigId());
                preparedStatement.setInt(2, item.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()));
                preparedStatement.setInt(3, item.endOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()));
                preparedStatement.setString(4, item.id());
                preparedStatement.setFloat(5, item.stability());

                updated += preparedStatement.executeUpdate();
                connection.commit();
            }catch (SQLException e) {
                if (LOGGER.isWarnEnabled())
                    LOGGER.warn("Error occurred, rolling back: {}", e.getMessage());
                connection.rollback();
            }
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            if (LOGGER.isWarnEnabled())
                LOGGER.warn("Error occurred: {}", e.getMessage());
        }

        return updated;
    }

}
