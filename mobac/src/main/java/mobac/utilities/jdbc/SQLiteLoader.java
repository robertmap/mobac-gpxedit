/*******************************************************************************
 * Copyright (c) MOBAC developers
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 ******************************************************************************/
package mobac.utilities.jdbc;

import mobac.utilities.I18nUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JOptionPane;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Dynamic loading of SqliteJDBC http://www.zentus.com/sqlitejdbc/
 * <p>
 * 2020-07-26: removed the load attempts for the old ch-werner.de JavaSQLite library which is no longer used.
 */
public class SQLiteLoader {

    private static final Logger log = LoggerFactory.getLogger(SQLiteLoader.class);
    private static final String SQLITE_DRIVERNAME2 = "org.sqlite.JDBC";
    private static boolean SQLITE_LOADED = false;

    public static boolean loadSQLiteOrShowError() {
        try {
            SQLiteLoader.loadSQLite();
            return true;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, SQLiteLoader.getMsgSqliteMissing(),
                    I18nUtils.localizedStringForKey("msg_environment_slqite_lib_missing_title"),
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    public static String getMsgSqliteMissing() {
        return I18nUtils.localizedStringForKey("msg_environment_slqite_lib_missing");
    }

    public static synchronized void loadSQLite() throws SQLException {
        SQLiteLoader.loadSQLite(SQLITE_DRIVERNAME2);
    }

    protected static synchronized void loadSQLite(String driverClassName) throws SQLException {
        if (SQLITE_LOADED) {
            return;
        }
        try {
            // Load the sqlite library
            Class.forName(driverClassName);
            SQLITE_LOADED = true;
            log.debug("SQLite library loaded. Driver class name: {}", driverClassName);
        } catch (Throwable t) {
            SQLException e = new SQLException(
                    "Loading of SQLite library failed (" + driverClassName + "): " + t.getMessage(), t);
            log.error(e.getMessage());
            throw e;
        }
    }

    public static void closeConnection(Connection conn) {
        if (conn == null) {
            return;
        }
        try {
            conn.close();
        } catch (Exception e) {
            log.error("Failed to close SQL connection: " + e.getMessage());
        }
    }

}
