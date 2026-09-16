package org.kumoc.database;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

public class CrearDataBase {

    public static void execute(Connection connection) throws IOException, SQLException {

        //-------ESTRUCTURA
        SqlScriptExecutor.execute(connection,"/db/schema.sql");

        //-------INDEX
        SqlScriptExecutor.execute(connection,"/db/indexes.sql");

        //-------TRIGGERS
        SqlScriptExecutor.execute(connection,"/db/triggers_stock.sql");
        SqlScriptExecutor.execute(connection,"/db/triggers_facturas.sql");

    }
}