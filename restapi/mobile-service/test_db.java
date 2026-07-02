import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class test_db {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://10.128.0.7:5436/ATENDIMENTO?sslmode=disable";
        String user = "rafael.vasconcelos";
        String password = "rafael25";

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            System.out.println("Conectado ao DB!");
            String sql = "SELECT column_name, data_type FROM information_schema.columns WHERE table_schema = 'consignacao' AND table_name = 'itemcontracheque'";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                System.out.println("itemcontracheque." + rs.getString("column_name") + " -> " + rs.getString("data_type"));
            }
            
            sql = "SELECT column_name, data_type FROM information_schema.columns WHERE table_schema = 'consignacao' AND table_name = 'servidor'";
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                System.out.println("servidor." + rs.getString("column_name") + " -> " + rs.getString("data_type"));
            }
            
            sql = "SELECT column_name, data_type FROM information_schema.columns WHERE table_schema = 'consignacao' AND table_name = 'operacao'";
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                System.out.println("operacao." + rs.getString("column_name") + " -> " + rs.getString("data_type"));
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
