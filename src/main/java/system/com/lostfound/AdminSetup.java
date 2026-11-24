
package system.com.lostfound;

import java.io.Console;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Scanner;

import org.mindrot.jbcrypt.BCrypt;

public class AdminSetup {

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);
        Console console = System.console();

        System.out.println("================================");
        System.out.println("       LSPU-HNN ADMIN SETUP");
        System.out.println("================================");

        System.out.print("Enter admin username: ");
        String username = scanner.nextLine().trim();

        System.out.print("Enter admin full name: ");
        String fullName = scanner.nextLine().trim();

        if (username.isEmpty() || fullName.isEmpty()) {
            System.out.println("Username and full name are required.");
            return;
        }

        char[] password;
        char[] confirmPassword;

        if (console != null) {
            password = console.readPassword("Enter admin password: ");
            confirmPassword = console.readPassword("Confirm password: ");
        } else {
            System.out.println(
                "WARNING: Password input will be visible in this terminal."
            );
            System.out.print("Enter admin password: ");
            password = scanner.nextLine().toCharArray();

            System.out.print("Confirm password: ");
            confirmPassword = scanner.nextLine().toCharArray();
        }

        if (password == null || confirmPassword == null) {
            System.out.println("Password input cancelled.");
            return;
        }

        try {
            if (password.length < 12) {
                System.out.println(
                    "Password must contain at least 12 characters."
                );
                return;
            }

            if (!Arrays.equals(password, confirmPassword)) {
                System.out.println("Passwords do not match.");
                return;
            }

            String passwordString = new String(password);

            String passwordHash = BCrypt.hashpw(
                passwordString,
                BCrypt.gensalt(12)
            );

            String sql =
                "INSERT INTO admins (username, password_hash, full_name) "
                + "VALUES (?, ?, ?)";

            try (Connection connection = DBConnection.getConnection()) {

                if (connection == null) {
                    System.out.println("Database connection failed.");
                    return;
                }

                try (PreparedStatement statement =
                        connection.prepareStatement(sql)) {

                    statement.setString(1, username);
                    statement.setString(2, passwordHash);
                    statement.setString(3, fullName);

                    statement.executeUpdate();

                    System.out.println();
                    System.out.println("ADMIN ACCOUNT CREATED!");
                    System.out.println("Username: " + username);
                    System.out.println("You can now log in as admin.");
                }

            } catch (SQLException e) {

                if (e.getErrorCode() == 1062) {
                    System.out.println(
                        "That admin username already exists."
                    );
                } else {
                    System.out.println("Could not create admin account.");
                    e.printStackTrace();
                }
            }

        } finally {
            Arrays.fill(password, '\0');
            Arrays.fill(confirmPassword, '\0');
        }
    }
}
