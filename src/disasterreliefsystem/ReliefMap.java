/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package disasterreliefsystem;

import java.awt.Color;
import java.awt.Component;
import java.util.List;
import javax.swing.JOptionPane;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author Neil
 */
public class ReliefMap extends javax.swing.JFrame {

    class EvacueeSearchResult {

        private String name;
        private int roomNumber;

        public EvacueeSearchResult(String name, int roomNumber) {
            this.name = name;
            this.roomNumber = roomNumber;
        }

        public String getName() {
            return name;
        }

        public int getRoomNumber() {
            return roomNumber;
        }

        @Override
        public String toString() {
            return name; // This determines what's shown in JList
        }
    }
    private class RoomInfo {
    int roomID;
    int maxCapacity;
    int currentOccupants;

    public RoomInfo(int roomID, int maxCapacity, int currentOccupants) {
        this.roomID = roomID;
        this.maxCapacity = maxCapacity;
        this.currentOccupants = currentOccupants;
    }
}

    private int selectedCenterID = 1;
    private int selectedRoomID = -1;

    /**
     * Creates new form Map
     */
    public ReliefMap() {
        initComponents();
        setLocationRelativeTo(null);
        jTable1.setDefaultRenderer(Object.class, new CustomTableCellRenderer());
        populatetable();
        countresident();
        loadRoomNumber();
        populateevacueetable();
        countdonation();
        jTabbedPane1.addChangeListener(e -> updateButtonVisibility());
        jList1.setModel(new DefaultListModel<>());
        updateButtonVisibility();

    }

    //design
    private void updateButtonVisibility() {
        int selectedIndex = jTabbedPane1.getSelectedIndex();
        jButton3.setVisible(selectedIndex == 2 || selectedIndex == 3|| selectedIndex == 4);
    }

    private class CustomTableCellRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {

            Component c = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);

            // Convert view row index to model row index
            int modelRow = table.convertRowIndexToModel(row);

            // Get category and expiration date from the model
            String category = (String) table.getModel().getValueAt(modelRow, 1);
            String expDateStr = (String) table.getModel().getValueAt(modelRow, 3);

            if ("PERISHABLE".equals(category) && !"N/A".equals(expDateStr)) {
                try {
                    LocalDate expDate = LocalDate.parse(expDateStr); // Parse the date string
                    LocalDate today = LocalDate.now();
                    long daysUntilExp = ChronoUnit.DAYS.between(today, expDate);

                    if (daysUntilExp < 0) {
                        c.setBackground(Color.RED); // Expired
                    } else if (daysUntilExp <= 30) {
                        c.setBackground(Color.ORANGE); // Expiring within a month
                    } else {
                        c.setBackground(Color.GREEN); // Valid
                    }
                } catch (DateTimeParseException e) {
                    // Handle invalid date format (optional)
                    c.setBackground(table.getBackground());
                }
            } else {
                c.setBackground(table.getBackground()); // Non-perishable or "N/A"
            }

            return c;
        }
    }

    //this is for the Donation
    private void donation() {
        String name = jTextField1.getText();
        String category = (String) jComboBox1.getSelectedItem();
        int quantity = (Integer) jSpinField1.getValue();
        java.util.Date selectedDate = jCalendar1.getDate();

        if (selectedDate == null) {
            JOptionPane.showMessageDialog(null, "Please select a registration date.");
            return;
        }
        if (name.isEmpty() || category == null || quantity <= 0) {
            JOptionPane.showMessageDialog(null, "Please Fill all the necessary Fields");
            return;
        }
        if ("PERISHABLE".equals(category) && selectedDate == null) {
            JOptionPane.showMessageDialog(null, "Expiration date is required for perishable items.");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            String query = "INSERT INTO donations(Name,Category,TotalInitialQuantity,CurrentQuantity,ExpirationDate,CenterID)VALUES(?,?,?,?,?,?)";
            PreparedStatement pst = conn.prepareStatement(query);
            pst.setString(1, name);
            pst.setString(2, category);
            pst.setInt(3, quantity);
            pst.setInt(4, quantity);
            if ("PERISHABLE".equals(category)) {
                pst.setDate(5, new java.sql.Date(selectedDate.getTime()));
            } else {
                pst.setNull(5, java.sql.Types.DATE); // Set NULL for non-perishable
            }
            pst.setInt(6, selectedCenterID);

            int rowsaffected = pst.executeUpdate();
            if (rowsaffected > 0) {
                JOptionPane.showMessageDialog(this, "Donation Added Successfully");
            } else {
                JOptionPane.showMessageDialog(this, "There was an error in inserting Donation");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void populatetable() {
        String[] columnNames = {"Donation Item", "Category", "Quantity", "Expiration Date"};
        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0);
        try (Connection conn = DatabaseConnection.getConnection()) {
            String query = "SELECT Name, Category, CurrentQuantity, ExpirationDate FROM donations WHERE CenterID = ?";
            PreparedStatement pst = conn.prepareStatement(query);
            pst.setInt(1, selectedCenterID);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                String name = rs.getString("Name");
                String cat = rs.getString("Category");
                int quan = rs.getInt("CurrentQuantity");
                java.sql.Date date = rs.getDate("ExpirationDate");
                String formattedDate = (date != null) ? date.toString() : "N/A";

                Object[] rowdata = {name, cat, quan, formattedDate};
                tableModel.addRow(rowdata);

            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        jTable1.setModel(tableModel);
    }

    private void toggleExpirationDate() {
        String category = (String) jComboBox1.getSelectedItem();
        boolean isPerishable = "PERISHABLE".equals(category);

        jCalendar1.setEnabled(isPerishable); // Disable for non-perishable
        jLabel13.setEnabled(isPerishable);   // Grey out label if needed

        if (!isPerishable) {
            jCalendar1.setCalendar(null); // Reset to today’s date but mark as unselected
        }
    }

    //this is for the room panel
    private void countresident() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String query = "SELECT COUNT(*) AS residentcount "
                    + "FROM Evacuees e "
                    + "JOIN Rooms r ON e.RoomID = r.RoomID "
                    + "JOIN EvacuationCenters c ON r.CenterID = c.CenterID "
                    + "WHERE c.CenterID = ?";
            PreparedStatement pst = conn.prepareStatement(query);
            pst.setInt(1, selectedCenterID);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                int count = rs.getInt("residentcount");
                jLabel15.setText(String.valueOf(count));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    private void countdonation() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String perishablequery = "SELECT COUNT(*) AS perishablefood FROM donations WHERE CenterID = ? AND Category = 'PERISHABLE'";
            PreparedStatement fpst = conn.prepareStatement(perishablequery);
            fpst.setInt(1, selectedCenterID);
            ResultSet frs = fpst.executeQuery();

            if (frs.next()) {
                int countperishablefood = frs.getInt("perishablefood");
                jLabel27.setText(String.valueOf(countperishablefood));
            }
            String nonperishablequery = "SELECT COUNT(*) AS nonperishablefood FROM donations WHERE CenterID = ? AND Category = 'NON-PERISHABLE'";
            PreparedStatement nfpst = conn.prepareStatement(nonperishablequery);
            nfpst.setInt(1, selectedCenterID);
            ResultSet nfrs = nfpst.executeQuery();

            if (nfrs.next()) {
                int countnonperishablefood = nfrs.getInt("nonperishablefood");
                jLabel29.setText(String.valueOf(countnonperishablefood));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    private void countResidentsInRoom() {
    try (Connection conn = DatabaseConnection.getConnection()) {
        String query = "SELECT COUNT(*) AS residentCount FROM Evacuees WHERE RoomID = ?";
        PreparedStatement pst = conn.prepareStatement(query);
        pst.setInt(1, selectedRoomID);  // Ensure selectedRoomID is the room's ID you want to count residents for
        ResultSet rs = pst.executeQuery();

        if (rs.next()) {
            int residentCount = rs.getInt("residentCount");
            jLabel33.setText(String.valueOf(residentCount)); // Update your UI component accordingly
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
}


   private void loadRoomsForCenter() {
    try (Connection conn = DatabaseConnection.getConnection()) {
        String query = "SELECT RoomID, RoomNumber, MaxCapacity, CurrentOccupants FROM Rooms WHERE CenterID = ?";
        PreparedStatement pst = conn.prepareStatement(query);
        pst.setInt(1, selectedCenterID);
        ResultSet rs = pst.executeQuery();

        Map<Integer, RoomInfo> roomNumberToInfo = new HashMap<>();
        while (rs.next()) {
            int roomID = rs.getInt("RoomID");
            int roomNumber = rs.getInt("RoomNumber");
            int maxCap = rs.getInt("MaxCapacity");
            int currentOcc = rs.getInt("CurrentOccupants");
            roomNumberToInfo.put(roomNumber, new RoomInfo(roomID, maxCap, currentOcc));
        }

        assignRoomIDsToButtons(roomNumberToInfo);
    } catch (SQLException e) {
        e.printStackTrace();
    }
}

   private void assignRoomIDsToButtons(Map<Integer, RoomInfo> roomNumberToInfo)  {
        // Example: jButton8 is labeled "ROOM 1" → RoomNumber=1
        resetRoomButtons();

        // Room 1 (jButton8)
       RoomInfo roomInfo = roomNumberToInfo.get(1);
    if (roomInfo != null) {
        jButton8.setActionCommand(String.valueOf(roomInfo.roomID));
        jButton8.setEnabled(true);
        jButton8.setText("ROOM 1");
        jButton8.setBackground(roomInfo.currentOccupants >= roomInfo.maxCapacity ? Color.RED : Color.GREEN);
        jButton8.setOpaque(true);
        jButton8.setBorderPainted(false);
    } else {
        jButton8.setEnabled(false);
        jButton8.setText("ROOM 1 (Unavailable)");
    }

        // Room 2 (jButton9)
        roomInfo = roomNumberToInfo.get(2);
    if (roomInfo != null) {
        jButton9.setActionCommand(String.valueOf(roomInfo.roomID));
        jButton9.setEnabled(true);
        jButton9.setText("ROOM 2");
        jButton9.setBackground(roomInfo.currentOccupants >= roomInfo.maxCapacity ? Color.RED : Color.GREEN);
        jButton9.setOpaque(true);
        jButton9.setBorderPainted(false);
    } else {
        jButton9.setEnabled(false);
        jButton9.setText("ROOM 2 (Unavailable)");
    }

        // Room 3 (jButton10)
        roomInfo = roomNumberToInfo.get(3);
    if (roomInfo != null) {
        jButton10.setActionCommand(String.valueOf(roomInfo.roomID));
        jButton10.setEnabled(true);
        jButton10.setText("ROOM 3");
        jButton10.setBackground(roomInfo.currentOccupants >= roomInfo.maxCapacity ? Color.RED : Color.GREEN);
        jButton10.setOpaque(true);
        jButton10.setBorderPainted(false);
    } else {
        jButton10.setEnabled(false);
        jButton10.setText("ROOM 3 (Unavailable)");
    }

        // Room 4 (jButton11)
         roomInfo = roomNumberToInfo.get(4);
    if (roomInfo != null) {
        jButton11.setActionCommand(String.valueOf(roomInfo.roomID));
        jButton11.setEnabled(true);
        jButton11.setText("ROOM 4");
        jButton11.setBackground(roomInfo.currentOccupants >= roomInfo.maxCapacity ? Color.RED : Color.GREEN);
        jButton11.setOpaque(true);
        jButton11.setBorderPainted(false);
    } else {
        jButton11.setEnabled(false);
        jButton11.setText("ROOM 4 (Unavailable)");
    }

        // Room 5 (jButton12)
        roomInfo = roomNumberToInfo.get(5);
    if (roomInfo != null) {
        jButton12.setActionCommand(String.valueOf(roomInfo.roomID));
        jButton12.setEnabled(true);
        jButton12.setText("ROOM 5");
        jButton12.setBackground(roomInfo.currentOccupants >= roomInfo.maxCapacity ? Color.RED : Color.GREEN);
        jButton12.setOpaque(true);
        jButton12.setBorderPainted(false);
    } else {
        jButton12.setEnabled(false);
        jButton12.setText("ROOM 5 (Unavailable)");
    }

        // Room 6 (jButton13)
         roomInfo = roomNumberToInfo.get(6);
    if (roomInfo != null) {
        jButton13.setActionCommand(String.valueOf(roomInfo.roomID));
        jButton13.setEnabled(true);
        jButton13.setText("ROOM 6");
        jButton13.setBackground(roomInfo.currentOccupants >= roomInfo.maxCapacity ? Color.RED : Color.GREEN);
        jButton13.setOpaque(true);
        jButton13.setBorderPainted(false);
    } else {
        jButton13.setEnabled(false);
        jButton13.setText("ROOM 6 (Unavailable)");
    }

        // Room 7 (jButton14)
          roomInfo = roomNumberToInfo.get(7);
    if (roomInfo != null) {
        jButton14.setActionCommand(String.valueOf(roomInfo.roomID));
        jButton14.setEnabled(true);
        jButton14.setText("ROOM 7");
        jButton14.setBackground(roomInfo.currentOccupants >= roomInfo.maxCapacity ? Color.RED : Color.GREEN);
        jButton14.setOpaque(true);
        jButton14.setBorderPainted(false);
    } else {
        jButton14.setEnabled(false);
        jButton14.setText("ROOM 7 (Unavailable)");
    }

        // Room 8 (jButton15)
          roomInfo = roomNumberToInfo.get(8);
    if (roomInfo != null) {
        jButton15.setActionCommand(String.valueOf(roomInfo.roomID));
        jButton15.setEnabled(true);
        jButton15.setText("ROOM 8");
        jButton15.setBackground(roomInfo.currentOccupants >= roomInfo.maxCapacity ? Color.RED : Color.GREEN);
        jButton15.setOpaque(true);
        jButton15.setBorderPainted(false);
    } else {
        jButton15.setEnabled(false);
        jButton15.setText("ROOM 8 (Unavailable)");
    }

        // Room 9 (jButton16)
          roomInfo = roomNumberToInfo.get(9);
    if (roomInfo != null) {
        jButton16.setActionCommand(String.valueOf(roomInfo.roomID));
        jButton16.setEnabled(true);
        jButton16.setText("ROOM 9");
        jButton16.setBackground(roomInfo.currentOccupants >= roomInfo.maxCapacity ? Color.RED : Color.GREEN);
        jButton16.setOpaque(true);
        jButton16.setBorderPainted(false);
    } else {
        jButton16.setEnabled(false);
        jButton16.setText("ROOM 9 (Unavailable)");
    }

        // Room 10 (jButton17)
          roomInfo = roomNumberToInfo.get(10);
    if (roomInfo != null) {
        jButton17.setActionCommand(String.valueOf(roomInfo.roomID));
        jButton17.setEnabled(true);
        jButton17.setText("ROOM 10");
        jButton17.setBackground(roomInfo.currentOccupants >= roomInfo.maxCapacity ? Color.RED : Color.GREEN);
        jButton17.setOpaque(true);
        jButton17.setBorderPainted(false);
    } else {
        jButton17.setEnabled(false);
        jButton17.setText("ROOM 10 (Unavailable)");
    }

        // Room 11 (jButton18)
         roomInfo = roomNumberToInfo.get(11);
    if (roomInfo != null) {
        jButton18.setActionCommand(String.valueOf(roomInfo.roomID));
        jButton18.setEnabled(true);
        jButton18.setText("ROOM 11");
        jButton18.setBackground(roomInfo.currentOccupants >= roomInfo.maxCapacity ? Color.RED : Color.GREEN);
        jButton18.setOpaque(true);
        jButton18.setBorderPainted(false);
    } else {
        jButton18.setEnabled(false);
        jButton18.setText("ROOM 11 (Unavailable)");
    }

    }

    private void resetRoomButtons() {
        JButton[] roomButtons = {jButton8, jButton9, jButton10, jButton11, jButton12,
            jButton13, jButton14, jButton15, jButton16, jButton17, jButton18};

        for (JButton button : roomButtons) {
            button.setEnabled(true);
            // Reset text to original label (e.g., "ROOM 1")
            button.setText(button.getText().replace(" (Unavailable)", ""));
        }
    }

    private void addEvacuee() {
        if (selectedRoomID == -1) {
            JOptionPane.showMessageDialog(this, "Select a room first!");
            return;
        }
        String name = jTextField2.getText().trim();
        int age = jSpinField2.getValue();
        String sex = (String) jComboBox2.getSelectedItem();
        String specialNeeds = jTextArea1.getText().trim();
        String origin = (String) jComboBox3.getSelectedItem();

        // Validate inputs
        if (name.isEmpty() || age <= 0 || sex == null) {
            JOptionPane.showMessageDialog(this, "Please fill all required fields");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            // Check capacity
            String capacityQuery = "SELECT MaxCapacity, CurrentOccupants FROM Rooms WHERE RoomID = ?";
            PreparedStatement capacityPst = conn.prepareStatement(capacityQuery);
            capacityPst.setInt(1, selectedRoomID);
            ResultSet rs = capacityPst.executeQuery();

            if (rs.next()) {
                int max = rs.getInt("MaxCapacity");
                int current = rs.getInt("CurrentOccupants");
                if (current >= max) {
                    JOptionPane.showMessageDialog(this, "Room is full!");
                    return;
                }
            }

            String insertQuery = "INSERT INTO evacuees(RoomID, Name, Age, Sex, SpecialNeeds, Origin) VALUES (?, ?, ?, ?, ?,?)";
            try (PreparedStatement pst = conn.prepareStatement(insertQuery)) {
                pst.setInt(1, selectedRoomID);
                pst.setString(2, name);
                pst.setInt(3, age);
                pst.setString(4, sex);
                pst.setString(5, specialNeeds);
                pst.setString(6, origin);

                int rowsAffected = pst.executeUpdate();
                if (rowsAffected > 0) {
                    // Update room occupancy
                    String updateQuery = "UPDATE Rooms SET CurrentOccupants = CurrentOccupants + 1 WHERE RoomID = ?";
                    try (PreparedStatement updatePst = conn.prepareStatement(updateQuery)) {
                        updatePst.setInt(1, selectedRoomID);
                        updatePst.executeUpdate();
                    }

                    JOptionPane.showMessageDialog(this, "Evacuee added successfully!");
                    clearForm();
                    countresident(); // Refresh total count
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage());
        }
    }

    private void clearForm() {
        jTextField2.setText("");
        jSpinField2.setValue(0);
        jComboBox2.setSelectedIndex(0);
        jTextArea1.setText("");
    }

    private void loadRoomNumber() {
        if (selectedRoomID == -1) {
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            String query = "SELECT RoomNumber FROM Rooms WHERE RoomID = ?";
            try (PreparedStatement pst = conn.prepareStatement(query)) {
                pst.setInt(1, selectedRoomID);
                ResultSet rs = pst.executeQuery();

                if (rs.next()) {
                    int roomNumber = rs.getInt("RoomNumber");
                    jLabel21.setText("Room " + roomNumber);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void populateevacueetable() {
        String[] columnNames = {"Name", "Age", "Origin"};
        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0);
        try (Connection conn = DatabaseConnection.getConnection()) {
            String query = "SELECT Name, Age, Origin FROM evacuees WHERE RoomID = ?";
            PreparedStatement pst = conn.prepareStatement(query);
            pst.setInt(1, selectedRoomID);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                String name = rs.getString("Name");
                int age = rs.getInt("Age");
                String sex = rs.getString("Origin");

                Object[] rowdata = {name, age, sex};
                tableModel.addRow(rowdata);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        jTable2.setModel(tableModel);
    }
// this is for the office

    private void fetchEvacuees() {
    String searchText = jTextField4.getText().trim();
    DefaultListModel<String> listModel = new DefaultListModel<>();
    List<EvacueeSearchResult> dataCache = new ArrayList<>();

    if (!searchText.isEmpty()) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String query = "SELECT e.Name, r.RoomNumber "
                    + "FROM Evacuees e "
                    + "JOIN Rooms r ON e.RoomID = r.RoomID "
                    + "JOIN EvacuationCenters c ON r.CenterID = c.CenterID "
                    + "WHERE e.Name LIKE ? AND c.CenterID = ?"; // Added center filter
                    
            PreparedStatement pst = conn.prepareStatement(query);
            pst.setString(1, "%" + searchText + "%");
            pst.setInt(2, selectedCenterID); // Use the current center ID
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                EvacueeSearchResult result = new EvacueeSearchResult(
                    rs.getString("Name"),
                    rs.getInt("RoomNumber")
                );
                dataCache.add(result);
                listModel.addElement(result.getName());
            }

            jList1.putClientProperty("evacueeData", dataCache);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
    jList1.setModel(listModel);
}

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jButton1 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        mapevacuationpanel = new javax.swing.JPanel();
        mappanel = new javax.swing.JPanel();
        jButton4 = new javax.swing.JButton();
        jButton5 = new javax.swing.JButton();
        jButton6 = new javax.swing.JButton();
        jLabel22 = new javax.swing.JLabel();
        jLabel23 = new javax.swing.JLabel();
        jLabel24 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        roompanel = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        jButton2 = new javax.swing.JButton();
        jButton7 = new javax.swing.JButton();
        jButton8 = new javax.swing.JButton();
        jButton9 = new javax.swing.JButton();
        jButton10 = new javax.swing.JButton();
        jButton11 = new javax.swing.JButton();
        jButton12 = new javax.swing.JButton();
        jButton13 = new javax.swing.JButton();
        jButton14 = new javax.swing.JButton();
        jButton15 = new javax.swing.JButton();
        jButton16 = new javax.swing.JButton();
        jButton17 = new javax.swing.JButton();
        jButton18 = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        jLabel26 = new javax.swing.JLabel();
        jLabel27 = new javax.swing.JLabel();
        jLabel28 = new javax.swing.JLabel();
        jLabel29 = new javax.swing.JLabel();
        registrarpanel = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        jComboBox1 = new javax.swing.JComboBox<>();
        jLabel11 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jSpinField1 = new com.toedter.components.JSpinField();
        jCalendar1 = new com.toedter.calendar.JCalendar();
        jLabel13 = new javax.swing.JLabel();
        jButton19 = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        jPanel6 = new javax.swing.JPanel();
        jLabel16 = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();
        jTextField2 = new javax.swing.JTextField();
        jSpinField2 = new com.toedter.components.JSpinField();
        jLabel18 = new javax.swing.JLabel();
        jLabel19 = new javax.swing.JLabel();
        jComboBox2 = new javax.swing.JComboBox<>();
        jLabel20 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTable2 = new javax.swing.JTable();
        jLabel21 = new javax.swing.JLabel();
        jButton20 = new javax.swing.JButton();
        jLabel25 = new javax.swing.JLabel();
        jComboBox3 = new javax.swing.JComboBox<>();
        jPanel10 = new javax.swing.JPanel();
        jLabel31 = new javax.swing.JLabel();
        jLabel33 = new javax.swing.JLabel();
        jPanel7 = new javax.swing.JPanel();
        jPanel8 = new javax.swing.JPanel();
        jPanel9 = new javax.swing.JPanel();
        jLabel30 = new javax.swing.JLabel();
        jLabel32 = new javax.swing.JLabel();
        jScrollPane4 = new javax.swing.JScrollPane();
        jList1 = new javax.swing.JList<>();
        jTextField4 = new javax.swing.JTextField();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0), 3));
        jPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel2.setBorder(javax.swing.BorderFactory.createMatteBorder(1, 0, 0, 1, new java.awt.Color(0, 0, 0)));

        jButton1.setText("Location");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jButton3.setText("Back");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 79, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton3))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(86, 86, 86)
                .addComponent(jButton1)
                .addGap(18, 18, 18)
                .addComponent(jButton3)
                .addContainerGap(498, Short.MAX_VALUE))
        );

        jPanel1.add(jPanel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(3, 3, 90, 649));

        mappanel.setBorder(javax.swing.BorderFactory.createMatteBorder(2, 2, 2, 2, new java.awt.Color(0, 0, 0)));
        mappanel.setLayout(null);

        jButton4.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/family.png"))); // NOI18N
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });
        mappanel.add(jButton4);
        jButton4.setBounds(620, 190, 80, 40);

        jButton5.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/family.png"))); // NOI18N
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
            }
        });
        mappanel.add(jButton5);
        jButton5.setBounds(330, 370, 70, 40);

        jButton6.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/family.png"))); // NOI18N
        jButton6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton6ActionPerformed(evt);
            }
        });
        mappanel.add(jButton6);
        jButton6.setBounds(350, 140, 70, 40);

        jLabel22.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel22.setText("CENTER A");
        mappanel.add(jLabel22);
        jLabel22.setBounds(620, 230, 100, 25);

        jLabel23.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel23.setText("CENTER B");
        mappanel.add(jLabel23);
        jLabel23.setBounds(320, 410, 100, 25);

        jLabel24.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel24.setText("CENTER C");
        mappanel.add(jLabel24);
        jLabel24.setBounds(340, 180, 100, 25);

        jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/map.png"))); // NOI18N
        mappanel.add(jLabel1);
        jLabel1.setBounds(10, 10, 840, 610);

        javax.swing.GroupLayout mapevacuationpanelLayout = new javax.swing.GroupLayout(mapevacuationpanel);
        mapevacuationpanel.setLayout(mapevacuationpanelLayout);
        mapevacuationpanelLayout.setHorizontalGroup(
            mapevacuationpanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mapevacuationpanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(mappanel, javax.swing.GroupLayout.PREFERRED_SIZE, 857, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(1779, Short.MAX_VALUE))
        );
        mapevacuationpanelLayout.setVerticalGroup(
            mapevacuationpanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mapevacuationpanelLayout.createSequentialGroup()
                .addGap(0, 16, Short.MAX_VALUE)
                .addComponent(mappanel, javax.swing.GroupLayout.PREFERRED_SIZE, 629, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        jTabbedPane1.addTab("tab1", mapevacuationpanel);

        jPanel5.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0), 3));
        jPanel5.setLayout(null);

        jButton2.setFont(new java.awt.Font("Segoe UI Black", 1, 14)); // NOI18N
        jButton2.setText("OFFICE");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });
        jPanel5.add(jButton2);
        jButton2.setBounds(380, 480, 140, 60);

        jButton7.setFont(new java.awt.Font("Segoe UI Black", 1, 14)); // NOI18N
        jButton7.setText("INVENTORY");
        jButton7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton7ActionPerformed(evt);
            }
        });
        jPanel5.add(jButton7);
        jButton7.setBounds(20, 480, 350, 120);

        jButton8.setText("ROOM 1");
        jButton8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton8ActionPerformed(evt);
            }
        });
        jPanel5.add(jButton8);
        jButton8.setBounds(620, 430, 120, 90);

        jButton9.setText("ROOM 2");
        jButton9.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton9ActionPerformed(evt);
            }
        });
        jPanel5.add(jButton9);
        jButton9.setBounds(620, 320, 120, 90);

        jButton10.setText("ROOM 3");
        jButton10.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton10ActionPerformed(evt);
            }
        });
        jPanel5.add(jButton10);
        jButton10.setBounds(620, 200, 120, 100);

        jButton11.setText("ROOM 4");
        jButton11.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton11ActionPerformed(evt);
            }
        });
        jPanel5.add(jButton11);
        jButton11.setBounds(620, 20, 120, 100);

        jButton12.setText("ROOM 5");
        jButton12.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton12ActionPerformed(evt);
            }
        });
        jPanel5.add(jButton12);
        jButton12.setBounds(480, 20, 120, 100);

        jButton13.setText("ROOM 6");
        jButton13.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton13ActionPerformed(evt);
            }
        });
        jPanel5.add(jButton13);
        jButton13.setBounds(340, 20, 130, 100);

        jButton14.setText("ROOM 7");
        jButton14.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton14ActionPerformed(evt);
            }
        });
        jPanel5.add(jButton14);
        jButton14.setBounds(210, 20, 120, 100);

        jButton15.setText("ROOM 8");
        jButton15.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton15ActionPerformed(evt);
            }
        });
        jPanel5.add(jButton15);
        jButton15.setBounds(240, 210, 130, 90);

        jButton16.setText("ROOM 9");
        jButton16.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton16ActionPerformed(evt);
            }
        });
        jPanel5.add(jButton16);
        jButton16.setBounds(110, 210, 120, 90);

        jButton17.setText("ROOM 10");
        jButton17.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton17ActionPerformed(evt);
            }
        });
        jPanel5.add(jButton17);
        jButton17.setBounds(110, 320, 120, 90);

        jButton18.setText("ROOM 11");
        jButton18.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton18ActionPerformed(evt);
            }
        });
        jPanel5.add(jButton18);
        jButton18.setBounds(240, 320, 130, 90);

        jLabel3.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel3.setText("EXIT");
        jPanel5.add(jLabel3);
        jLabel3.setBounds(100, 20, 40, 16);

        jLabel4.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel4.setText("ENTRANCE");
        jPanel5.add(jLabel4);
        jLabel4.setBounds(540, 580, 70, 16);

        jLabel5.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel5.setText("REST ROOM");
        jPanel5.add(jLabel5);
        jLabel5.setBounds(410, 220, 90, 16);

        jLabel6.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel6.setText("REST ROOM");
        jPanel5.add(jLabel6);
        jLabel6.setBounds(410, 370, 90, 16);

        jLabel7.setFont(new java.awt.Font("Segoe UI Black", 1, 14)); // NOI18N
        jLabel7.setText("FEMALE");
        jPanel5.add(jLabel7);
        jLabel7.setBounds(410, 240, 60, 50);

        jLabel8.setFont(new java.awt.Font("Segoe UI Black", 1, 14)); // NOI18N
        jLabel8.setText("MALE");
        jPanel5.add(jLabel8);
        jLabel8.setBounds(420, 400, 50, 30);

        jLabel2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/room.png"))); // NOI18N
        jPanel5.add(jLabel2);
        jLabel2.setBounds(0, 10, 760, 600);

        jLabel14.setText("Total Evacuees");
        jPanel5.add(jLabel14);
        jLabel14.setBounds(810, 30, 90, 16);

        jLabel15.setFont(new java.awt.Font("Segoe UI Black", 1, 24)); // NOI18N
        jLabel15.setText("0");
        jPanel5.add(jLabel15);
        jLabel15.setBounds(840, 50, 30, 50);

        jLabel26.setText("Total Perishable");
        jPanel5.add(jLabel26);
        jLabel26.setBounds(800, 110, 90, 16);

        jLabel27.setFont(new java.awt.Font("Segoe UI Black", 1, 24)); // NOI18N
        jLabel27.setText("0");
        jPanel5.add(jLabel27);
        jLabel27.setBounds(840, 130, 40, 50);

        jLabel28.setText("Total Non-Perishable");
        jPanel5.add(jLabel28);
        jLabel28.setBounds(790, 190, 120, 16);

        jLabel29.setFont(new java.awt.Font("Segoe UI Black", 1, 24)); // NOI18N
        jLabel29.setText("0");
        jPanel5.add(jLabel29);
        jLabel29.setBounds(840, 220, 40, 30);

        javax.swing.GroupLayout roompanelLayout = new javax.swing.GroupLayout(roompanel);
        roompanel.setLayout(roompanelLayout);
        roompanelLayout.setHorizontalGroup(
            roompanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roompanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, 955, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(1681, Short.MAX_VALUE))
        );
        roompanelLayout.setVerticalGroup(
            roompanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, roompanelLayout.createSequentialGroup()
                .addContainerGap(18, Short.MAX_VALUE)
                .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, 612, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(15, 15, 15))
        );

        jTabbedPane1.addTab("tab2", roompanel);

        jPanel4.setBorder(javax.swing.BorderFactory.createMatteBorder(2, 2, 2, 2, new java.awt.Color(0, 0, 0)));

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane1.setViewportView(jTable1);

        jLabel9.setText("Donation");

        jLabel10.setText("Donation Name:");

        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "PERISHABLE", "NON-PERISHABLE" }));
        jComboBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox1ActionPerformed(evt);
            }
        });

        jLabel11.setText("Category:");

        jLabel12.setText("Quantity:");

        jLabel13.setText("Expiration Date:");

        jButton19.setText("SUBMIT");
        jButton19.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton19ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, 102, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel11, javax.swing.GroupLayout.PREFERRED_SIZE, 59, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addComponent(jLabel13, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jLabel12, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel4Layout.createSequentialGroup()
                                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jCalendar1, javax.swing.GroupLayout.DEFAULT_SIZE, 245, Short.MAX_VALUE)
                                    .addGroup(jPanel4Layout.createSequentialGroup()
                                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 162, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(jSpinField1, javax.swing.GroupLayout.PREFERRED_SIZE, 108, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addGap(0, 0, Short.MAX_VALUE)))
                                .addGap(36, 36, 36))
                            .addGroup(jPanel4Layout.createSequentialGroup()
                                .addComponent(jButton19)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(24, 24, 24))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addComponent(jLabel9)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel10)
                            .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel11)
                            .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel12)
                            .addComponent(jSpinField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jCalendar1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel13))
                        .addGap(18, 18, 18)
                        .addComponent(jButton19))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 274, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(319, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout registrarpanelLayout = new javax.swing.GroupLayout(registrarpanel);
        registrarpanel.setLayout(registrarpanelLayout);
        registrarpanelLayout.setHorizontalGroup(
            registrarpanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(registrarpanelLayout.createSequentialGroup()
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 1767, Short.MAX_VALUE))
        );
        registrarpanelLayout.setVerticalGroup(
            registrarpanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(registrarpanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("tab3", registrarpanel);

        jPanel6.setBorder(javax.swing.BorderFactory.createMatteBorder(3, 3, 3, 3, new java.awt.Color(0, 0, 0)));

        jLabel16.setText("Evacuees");

        jLabel17.setText("Name:");

        jLabel18.setText("Age:");

        jLabel19.setText("Sex:");

        jComboBox2.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Male", "Female", " " }));

        jLabel20.setText("Special Needs:");

        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jScrollPane2.setViewportView(jTextArea1);

        jTable2.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane3.setViewportView(jTable2);

        jLabel21.setFont(new java.awt.Font("Segoe UI Black", 1, 24)); // NOI18N

        jButton20.setText("Add");
        jButton20.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton20ActionPerformed(evt);
            }
        });

        jLabel25.setText("Origin:");

        jComboBox3.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Balibago Calatagan", "Talisay Calatagan", "Caritunan Calatagan", "Balitoc Calatagan", "Barangay 7 Calatagan", "Hukay Calatagan", "Lucsuhin Calatagan", "Gulod Calatagan", "Calambuyan Calatagan", " " }));

        jPanel10.setBorder(javax.swing.BorderFactory.createMatteBorder(3, 3, 3, 3, new java.awt.Color(0, 0, 0)));

        jLabel31.setFont(new java.awt.Font("Segoe UI Black", 1, 18)); // NOI18N
        jLabel31.setText("CAPACITY");

        jLabel33.setFont(new java.awt.Font("Segoe UI Black", 1, 48)); // NOI18N
        jLabel33.setText("0");

        javax.swing.GroupLayout jPanel10Layout = new javax.swing.GroupLayout(jPanel10);
        jPanel10.setLayout(jPanel10Layout);
        jPanel10Layout.setHorizontalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel10Layout.createSequentialGroup()
                .addContainerGap(146, Short.MAX_VALUE)
                .addComponent(jLabel31, javax.swing.GroupLayout.PREFERRED_SIZE, 114, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(140, 140, 140))
            .addGroup(jPanel10Layout.createSequentialGroup()
                .addGap(179, 179, 179)
                .addComponent(jLabel33, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel10Layout.setVerticalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel10Layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addComponent(jLabel31, javax.swing.GroupLayout.PREFERRED_SIZE, 56, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel33, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(96, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addGap(61, 61, 61)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(jLabel16, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jLabel17, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jLabel18, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel19, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel25, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(21, 21, 21)
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, 240, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, 84, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addComponent(jComboBox3, javax.swing.GroupLayout.Alignment.LEADING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jSpinField2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addComponent(jLabel20)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 303, Short.MAX_VALUE)))
                .addGap(18, 18, 18)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 438, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(26, 26, 26))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel6Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel6Layout.createSequentialGroup()
                        .addComponent(jLabel21, javax.swing.GroupLayout.PREFERRED_SIZE, 137, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(168, 168, 168)
                        .addComponent(jPanel10, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(51, 51, 51))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel6Layout.createSequentialGroup()
                        .addComponent(jButton20)
                        .addGap(611, 611, 611))))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addGap(56, 56, 56)
                        .addComponent(jLabel21, javax.swing.GroupLayout.PREFERRED_SIZE, 88, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(57, 57, 57)
                        .addComponent(jLabel16))
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jPanel10, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel17)
                            .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jSpinField2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel18))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel19)
                            .addComponent(jComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel25)
                            .addComponent(jComboBox3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel20)
                            .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 173, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jButton20)
                .addContainerGap(190, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(1694, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("tab4", jPanel3);

        jPanel8.setBorder(javax.swing.BorderFactory.createMatteBorder(4, 4, 4, 4, new java.awt.Color(0, 0, 0)));

        jPanel9.setBorder(javax.swing.BorderFactory.createMatteBorder(3, 0, 0, 0, new java.awt.Color(0, 0, 0)));

        jLabel30.setText("Daily Supplies:");

        javax.swing.GroupLayout jPanel9Layout = new javax.swing.GroupLayout(jPanel9);
        jPanel9.setLayout(jPanel9Layout);
        jPanel9Layout.setHorizontalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel9Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel30, javax.swing.GroupLayout.PREFERRED_SIZE, 79, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel9Layout.setVerticalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel9Layout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addComponent(jLabel30)
                .addContainerGap(391, Short.MAX_VALUE))
        );

        jLabel32.setText("Search for a Person:");

        jList1.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                jList1ValueChanged(evt);
            }
        });
        jScrollPane4.setViewportView(jList1);

        jTextField4.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                jTextField4KeyReleased(evt);
            }
        });

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel9, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addGap(80, 80, 80)
                .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel32, javax.swing.GroupLayout.PREFERRED_SIZE, 123, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextField4, javax.swing.GroupLayout.PREFERRED_SIZE, 194, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(40, 40, 40)
                .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 309, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(326, Short.MAX_VALUE))
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel8Layout.createSequentialGroup()
                .addGap(31, 31, 31)
                .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel8Layout.createSequentialGroup()
                        .addComponent(jLabel32)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextField4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 41, Short.MAX_VALUE)
                .addComponent(jPanel9, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(20, 20, 20))
        );

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addComponent(jPanel8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 1685, Short.MAX_VALUE))
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("tab5", jPanel7);

        jPanel1.add(jTabbedPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(126, -34, -1, 680));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 1093, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        // TODO add your handling code here:
        jTabbedPane1.setSelectedIndex(0);
        updateButtonVisibility();
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        // TODO add your handling code here:
        selectedCenterID = 1;
        jTabbedPane1.setSelectedIndex(1);
        loadRoomsForCenter();
        populatetable();
        countresident();
        countdonation();
        updateButtonVisibility();

    }//GEN-LAST:event_jButton4ActionPerformed

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed
        // TODO add your handling code here:
        selectedCenterID = 2;
        jTabbedPane1.setSelectedIndex(1);
        loadRoomsForCenter();
        populatetable();
        countresident();
        countdonation();
        updateButtonVisibility();
    }//GEN-LAST:event_jButton5ActionPerformed

    private void jButton6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton6ActionPerformed
        // TODO add your handling code here:
        selectedCenterID = 3;
        jTabbedPane1.setSelectedIndex(1);
        loadRoomsForCenter();
        populatetable();
        countresident();
        countdonation();
        updateButtonVisibility();
    }//GEN-LAST:event_jButton6ActionPerformed

    private void jButton19ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton19ActionPerformed
        // TODO add your handling code here:
        donation();
        populatetable();
    }//GEN-LAST:event_jButton19ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        // TODO add your handling code here:
        jTabbedPane1.setSelectedIndex(4);
        updateButtonVisibility();
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jComboBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox1ActionPerformed
        // TODO add your handling code here:
        toggleExpirationDate();
    }//GEN-LAST:event_jComboBox1ActionPerformed

    private void jButton7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton7ActionPerformed
        // TODO add your handling code here:
        jTabbedPane1.setSelectedIndex(2);
        updateButtonVisibility();
    }//GEN-LAST:event_jButton7ActionPerformed

    private void jButton8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton8ActionPerformed
        // TODO add your handling code here:
        selectedRoomID = Integer.parseInt(jButton8.getActionCommand());
        jTabbedPane1.setSelectedIndex(3);
        loadRoomNumber();
        populateevacueetable();
        updateButtonVisibility();
        countResidentsInRoom();

    }//GEN-LAST:event_jButton8ActionPerformed

    private void jButton9ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton9ActionPerformed
        // TODO add your handling code here:
        selectedRoomID = Integer.parseInt(jButton9.getActionCommand());
        jTabbedPane1.setSelectedIndex(3);
        loadRoomNumber();
        populateevacueetable();
        updateButtonVisibility();
        countResidentsInRoom();
    }//GEN-LAST:event_jButton9ActionPerformed

    private void jButton10ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton10ActionPerformed
        // TODO add your handling code here:
        selectedRoomID = Integer.parseInt(jButton10.getActionCommand());
        jTabbedPane1.setSelectedIndex(3);
        loadRoomNumber();
        populateevacueetable();
        updateButtonVisibility();
        countResidentsInRoom();
    }//GEN-LAST:event_jButton10ActionPerformed

    private void jButton11ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton11ActionPerformed
        // TODO add your handling code here:
        selectedRoomID = Integer.parseInt(jButton11.getActionCommand());
        jTabbedPane1.setSelectedIndex(3);
        loadRoomNumber();
        populateevacueetable();
        updateButtonVisibility();
        countResidentsInRoom();
    }//GEN-LAST:event_jButton11ActionPerformed

    private void jButton12ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton12ActionPerformed
        // TODO add your handling code here:
        selectedRoomID = Integer.parseInt(jButton12.getActionCommand());
        jTabbedPane1.setSelectedIndex(3);
        loadRoomNumber();
        populateevacueetable();
        updateButtonVisibility();
        countResidentsInRoom();
    }//GEN-LAST:event_jButton12ActionPerformed

    private void jButton13ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton13ActionPerformed
        // TODO add your handling code here:
        selectedRoomID = Integer.parseInt(jButton13.getActionCommand());
        jTabbedPane1.setSelectedIndex(3);
        loadRoomNumber();
        populateevacueetable();
        updateButtonVisibility();
        countResidentsInRoom();
    }//GEN-LAST:event_jButton13ActionPerformed

    private void jButton14ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton14ActionPerformed
        // TODO add your handling code here:
        selectedRoomID = Integer.parseInt(jButton14.getActionCommand());
        jTabbedPane1.setSelectedIndex(3);
        loadRoomNumber();
        populateevacueetable();
        updateButtonVisibility();
        countResidentsInRoom();
    }//GEN-LAST:event_jButton14ActionPerformed

    private void jButton15ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton15ActionPerformed
        // TODO add your handling code here:
        selectedRoomID = Integer.parseInt(jButton15.getActionCommand());
        jTabbedPane1.setSelectedIndex(3);
        loadRoomNumber();
        populateevacueetable();
        updateButtonVisibility();
        countResidentsInRoom();
    }//GEN-LAST:event_jButton15ActionPerformed

    private void jButton16ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton16ActionPerformed
        // TODO add your handling code here:
        selectedRoomID = Integer.parseInt(jButton16.getActionCommand());
        jTabbedPane1.setSelectedIndex(3);
        loadRoomNumber();
        populateevacueetable();
        updateButtonVisibility();
        countResidentsInRoom();
    }//GEN-LAST:event_jButton16ActionPerformed

    private void jButton17ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton17ActionPerformed
        // TODO add your handling code here:
        selectedRoomID = Integer.parseInt(jButton17.getActionCommand());
        jTabbedPane1.setSelectedIndex(3);
        loadRoomNumber();
        populateevacueetable();
        updateButtonVisibility();
        countResidentsInRoom();

    }//GEN-LAST:event_jButton17ActionPerformed

    private void jButton18ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton18ActionPerformed
        // TODO add your handling code here:
        selectedRoomID = Integer.parseInt(jButton18.getActionCommand());
        jTabbedPane1.setSelectedIndex(3);
        loadRoomNumber();
        populateevacueetable();
        updateButtonVisibility();
        countResidentsInRoom();
    }//GEN-LAST:event_jButton18ActionPerformed

    private void jButton20ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton20ActionPerformed
        // TODO add your handling code here:
        addEvacuee();
        populateevacueetable();
    }//GEN-LAST:event_jButton20ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        // TODO add your handling code here:
        jTabbedPane1.setSelectedIndex(1);
        loadRoomsForCenter();
        populatetable();
        countresident();
        countdonation();
        updateButtonVisibility();
    }//GEN-LAST:event_jButton3ActionPerformed

    private void jTextField4KeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextField4KeyReleased
        fetchEvacuees();
    }//GEN-LAST:event_jTextField4KeyReleased

    private void jList1ValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_jList1ValueChanged
        if (!evt.getValueIsAdjusting()) {
            int selectedIndex = jList1.getSelectedIndex();
            if (selectedIndex >= 0) {
                // Add explicit cast here
                List<EvacueeSearchResult> data = (List<EvacueeSearchResult>) jList1.getClientProperty("evacueeData");

                if (data != null && selectedIndex < data.size()) {
                    EvacueeSearchResult selected = data.get(selectedIndex);
                    JOptionPane.showMessageDialog(this,
                            "Name: " + selected.getName() + "\nRoom: "
                            + (selected.getRoomNumber() > 0 ? selected.getRoomNumber() : "Not assigned"),
                            "Location Info",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            }
        }
    }//GEN-LAST:event_jList1ValueChanged

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(ReliefMap.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(ReliefMap.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(ReliefMap.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(ReliefMap.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new ReliefMap().setVisible(true);
            }
        });

    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton10;
    private javax.swing.JButton jButton11;
    private javax.swing.JButton jButton12;
    private javax.swing.JButton jButton13;
    private javax.swing.JButton jButton14;
    private javax.swing.JButton jButton15;
    private javax.swing.JButton jButton16;
    private javax.swing.JButton jButton17;
    private javax.swing.JButton jButton18;
    private javax.swing.JButton jButton19;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton20;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JButton jButton7;
    private javax.swing.JButton jButton8;
    private javax.swing.JButton jButton9;
    private com.toedter.calendar.JCalendar jCalendar1;
    private javax.swing.JComboBox<String> jComboBox1;
    private javax.swing.JComboBox<String> jComboBox2;
    private javax.swing.JComboBox<String> jComboBox3;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel28;
    private javax.swing.JLabel jLabel29;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel30;
    private javax.swing.JLabel jLabel31;
    private javax.swing.JLabel jLabel32;
    private javax.swing.JLabel jLabel33;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JList<String> jList1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private com.toedter.components.JSpinField jSpinField1;
    private com.toedter.components.JSpinField jSpinField2;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTable jTable1;
    private javax.swing.JTable jTable2;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JTextField jTextField4;
    private javax.swing.JPanel mapevacuationpanel;
    private javax.swing.JPanel mappanel;
    private javax.swing.JPanel registrarpanel;
    private javax.swing.JPanel roompanel;
    // End of variables declaration//GEN-END:variables
}
