import java.sql.*;

public class test_db {
    public static void main(String[] args) {
        try {
            Connection conn = DriverManager.getConnection("jdbc:postgresql://10.128.0.7:5436/ATENDIMENTO?sslmode=disable", "rafael.vasconcelos", "rafael25");
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT table_schema, table_name FROM information_schema.tables WHERE table_name ilike '%contracheque%'");
            while (rs.next()) {
                System.out.println("Schema: " + rs.getString(1) + " Table: " + rs.getString(2));
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
