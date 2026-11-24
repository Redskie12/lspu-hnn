
package system.com.lostfound;

public class Session {

    private static String studentId;
    private static String adminUsername;
    private static String role;

    // STUDENT SESSION
    public static void setStudentId(String id) {
        studentId = id;
        adminUsername = null;
        role = "STUDENT";
    }

    // ADMIN SESSION
    public static void setAdmin(String username) {
        adminUsername = username;
        studentId = null;
        role = "ADMIN";
    }

    public static String getStudentId() {
        return studentId;
    }

    public static String getAdminUsername() {
        return adminUsername;
    }

    public static String getRole() {
        return role;
    }

    public static boolean isAdmin() {
        return "ADMIN".equals(role);
    }

    public static boolean isStudent() {
        return "STUDENT".equals(role);
    }

    public static String getMaskedStudentId() {

        if (studentId == null || studentId.length() < 2) {
            return "****";
        }

        return studentId.charAt(0)
                + "*".repeat(studentId.length() - 2)
                + studentId.charAt(studentId.length() - 1);
    }

    public static void clearSession() {
        studentId = null;
        adminUsername = null;
        role = null;
    }
}
