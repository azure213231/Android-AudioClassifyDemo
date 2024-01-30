//package com.demo.ncnndemo;
//import javax.swing.JFrame;
//public class CloudDrawing extends JFrame {
//    public CloudDrawing() {
//        setTitle("Cloud Pattern");
//        setSize(400, 300);
//        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        setLocationRelativeTo(null);
//        setVisible(true);
//    }
//
//    @Override
//    public void paint(Graphics g) {
//        super.paint(g);
//        Graphics2D g2d = (Graphics2D) g;
//        g2d.setColor(Color.WHITE);
//        g2d.fillOval(50, 80, 100, 50);
//        g2d.fillOval(120, 70, 100, 60);
//        g2d.fillOval(190, 80, 100, 50);
//        g2d.fillOval(110, 105, 100, 50);
//        g2d.fillOval(160, 115, 100, 50);
//    }
//
//    public static void main(String[] args) {
//        SwingUtilities.invokeLater(() -> {
//            new CloudDrawing();
//        });
//    }
//}