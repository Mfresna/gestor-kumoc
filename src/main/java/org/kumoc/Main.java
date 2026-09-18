package org.kumoc;

import org.kumoc.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class Main {

    static void main(String[] args) {

        try (Connection connection = DatabaseManager.initialize()) {

            System.out.println("Conectado correctamente.");

            //insertarPersona(connection, "Matias", 30);

            //mostrarPersonas(connection);

        } catch (Exception e) {

            e.printStackTrace();
        }
    }


    private static void insertarPersona(Connection connection, String nombre, int edad) throws Exception {

        String sql =
                "INSERT INTO PERSONAS(nombre, edad) VALUES (?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, nombre);
            statement.setInt(2, edad);

            statement.executeUpdate();
        }

        System.out.println(
                "Persona guardada."
        );
    }

    private static void mostrarPersonas(Connection connection) throws Exception {

        String sql =
                "SELECT * FROM PERSONAS";

        try (Statement statement =
                     connection.createStatement();

             ResultSet resultSet = statement.executeQuery(sql)) {

            System.out.println();
            System.out.println("PERSONAS:");

            while (resultSet.next()) {

                int id =
                        resultSet.getInt("id");

                String nombre =
                        resultSet.getString("nombre");

                int edad =
                        resultSet.getInt("edad");

                System.out.println(
                        id + " | "
                                + nombre + " | "
                                + edad
                );
            }
        }
    }
}