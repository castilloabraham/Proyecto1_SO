/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Interfaces;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author adcd_
 */
public class Graficocpu extends JFrame {

    // ── Datos históricos ──────────────────────────────────────────────
    // true = CPU ocupado (proceso de usuario), false = CPU libre (SO)
    private final List<Boolean> historialCPU = new ArrayList<>();
    private final List<Integer> historialCiclos = new ArrayList<>();

    private final PanelGrafico panelGrafico;
    private final JLabel labelUtilizacion;
    private final JLabel labelCiclosTotal;

    private static Graficocpu instancia;

    // ── Constructor ───────────────────────────────────────────────────
    private Graficocpu() {
        super("Gráfico de Utilización del CPU");
        setSize(800, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

        // Panel principal
        JPanel contenedor = new JPanel(new BorderLayout(5, 5));
        contenedor.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Panel del gráfico
        panelGrafico = new PanelGrafico();
        panelGrafico.setPreferredSize(new Dimension(760, 280));
        panelGrafico.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        // Panel de métricas abajo
        JPanel panelMetricas = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 5));
        panelMetricas.setBackground(new Color(245, 245, 245));

        labelUtilizacion = new JLabel("Utilización CPU: 0.0%");
        labelUtilizacion.setFont(new Font("Dialog", Font.BOLD, 13));

        labelCiclosTotal = new JLabel("Ciclos totales: 0  |  Ciclos activos: 0");
        labelCiclosTotal.setFont(new Font("Dialog", Font.PLAIN, 12));

        // Leyenda
        JLabel leyendaOcupado = new JLabel("█ CPU Ocupado");
        leyendaOcupado.setForeground(new Color(0, 140, 0));
        leyendaOcupado.setFont(new Font("Dialog", Font.PLAIN, 11));

        JLabel leyendaLibre = new JLabel("█ CPU Libre (SO)");
        leyendaLibre.setForeground(new Color(200, 60, 60));
        leyendaLibre.setFont(new Font("Dialog", Font.PLAIN, 11));

        panelMetricas.add(labelUtilizacion);
        panelMetricas.add(labelCiclosTotal);
        panelMetricas.add(leyendaOcupado);
        panelMetricas.add(leyendaLibre);

        // Botón limpiar
        JButton btnLimpiar = new JButton("Limpiar historial");
        btnLimpiar.addActionListener(e -> limpiar());
        panelMetricas.add(btnLimpiar);

        contenedor.add(panelGrafico, BorderLayout.CENTER);
        contenedor.add(panelMetricas, BorderLayout.SOUTH);
        add(contenedor);
    }

    // ── Singleton ─────────────────────────────────────────────────────
    public static Graficocpu getInstance() {
        if (instancia == null) instancia = new Graficocpu();
        return instancia;
    }

    // ── API pública ───────────────────────────────────────────────────
    /**
     * Llamar cada tick del reloj desde InterfazHome.
     * @param ciclo número de ciclo actual
     * @param cpuOcupado true si hay proceso de usuario corriendo
     */
    public void registrarCiclo(int ciclo, boolean cpuOcupado) {
        historialCPU.add(cpuOcupado);
        historialCiclos.add(ciclo);

        // Calcular utilización
        long activos = historialCPU.stream().filter(b -> b).count();
        double util = historialCPU.isEmpty() ? 0.0 :
            (activos * 100.0) / historialCPU.size();

        // Actualizar UI solo si la ventana está visible (para no gastar recursos)
        if (isVisible()) {
            SwingUtilities.invokeLater(() -> {
                labelUtilizacion.setText(String.format("Utilización CPU: %.1f%%", util));
                labelCiclosTotal.setText("Ciclos totales: " + historialCPU.size()
                    + "  |  Ciclos activos: " + activos);
                panelGrafico.repaint();
            });
        }
    }

    public void limpiar() {
        historialCPU.clear();
        historialCiclos.clear();
        SwingUtilities.invokeLater(() -> {
            labelUtilizacion.setText("Utilización CPU: 0.0%");
            labelCiclosTotal.setText("Ciclos totales: 0  |  Ciclos activos: 0");
            panelGrafico.repaint();
        });
    }

    // ── Panel de dibujo ───────────────────────────────────────────────
    private class PanelGrafico extends JPanel {

        private static final int MARGEN_IZQ  = 50;
        private static final int MARGEN_DER  = 20;
        private static final int MARGEN_ARR  = 20;
        private static final int MARGEN_ABA  = 40;

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int anchoGraf = w - MARGEN_IZQ - MARGEN_DER;
            int altoGraf  = h - MARGEN_ARR - MARGEN_ABA;

            // Fondo blanco del área de gráfico
            g2.setColor(Color.WHITE);
            g2.fillRect(MARGEN_IZQ, MARGEN_ARR, anchoGraf, altoGraf);

            // Líneas de cuadrícula horizontales
            g2.setColor(new Color(220, 220, 220));
            g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_BEVEL, 0, new float[]{4}, 0));
            int yMitad = MARGEN_ARR + altoGraf / 2;
            g2.drawLine(MARGEN_IZQ, yMitad, MARGEN_IZQ + anchoGraf, yMitad);

            // Ejes
            g2.setColor(Color.DARK_GRAY);
            g2.setStroke(new BasicStroke(1.5f));
            // Eje Y
            g2.drawLine(MARGEN_IZQ, MARGEN_ARR,
                        MARGEN_IZQ, MARGEN_ARR + altoGraf);
            // Eje X
            g2.drawLine(MARGEN_IZQ, MARGEN_ARR + altoGraf,
                        MARGEN_IZQ + anchoGraf, MARGEN_ARR + altoGraf);

            // Labels eje Y
            g2.setFont(new Font("Dialog", Font.PLAIN, 10));
            g2.drawString("100%", 2, MARGEN_ARR + 5);
            g2.drawString(" 50%", 2, yMitad + 5);
            g2.drawString("  0%", 2, MARGEN_ARR + altoGraf + 5);

            // Label eje X
            g2.setFont(new Font("Dialog", Font.PLAIN, 10));
            g2.drawString("Tiempo (ciclos)", MARGEN_IZQ + anchoGraf / 2 - 35,
                h - 5);

            // Título
            g2.setFont(new Font("Dialog", Font.BOLD, 12));
            g2.setColor(Color.DARK_GRAY);
            g2.drawString("Utilización del CPU vs Tiempo", MARGEN_IZQ + 10, MARGEN_ARR - 5);

            // ── Dibujar datos ──────────────────────────────────────
            if (historialCPU.isEmpty()) {
                g2.setColor(Color.LIGHT_GRAY);
                g2.setFont(new Font("Dialog", Font.ITALIC, 13));
                g2.drawString("Sin datos — inicia la simulación",
                    MARGEN_IZQ + anchoGraf / 2 - 100, MARGEN_ARR + altoGraf / 2);
                return;
            }

            int n = historialCPU.size();
            // Cuántos ciclos mostrar como máximo (ventana deslizante)
            int maxVisible = Math.max(1, anchoGraf / 6);
            int inicio = Math.max(0, n - maxVisible);

            List<Boolean> visible = historialCPU.subList(inicio, n);
            List<Integer>  ciclos = historialCiclos.subList(inicio, n);

            double anchoPunto = (double) anchoGraf / Math.max(visible.size(), 1);

            // Dibujar barras rellenas (área bajo la curva)
            for (int i = 0; i < visible.size(); i++) {
                boolean ocupado = visible.get(i);
                int x = MARGEN_IZQ + (int)(i * anchoPunto);
                int ancho = Math.max(1, (int) anchoPunto);

                if (ocupado) {
                    // Verde para CPU ocupado
                    g2.setColor(new Color(0, 180, 0, 180));
                    g2.fillRect(x, MARGEN_ARR, ancho, altoGraf);
                    g2.setColor(new Color(0, 140, 0));
                    g2.fillRect(x, MARGEN_ARR, ancho, altoGraf);
                } else {
                    // Rojo suave para CPU libre
                    g2.setColor(new Color(220, 80, 80, 120));
                    g2.fillRect(x, MARGEN_ARR, ancho, altoGraf);
                }
            }

            // Dibujar línea de utilización acumulada encima
            g2.setColor(new Color(0, 80, 200));
            g2.setStroke(new BasicStroke(2f));
            int[] xs = new int[visible.size()];
            int[] ys = new int[visible.size()];
            int activos = 0;
            for (int i = 0; i < visible.size(); i++) {
                if (visible.get(i)) activos++;
                double util = (activos * 1.0) / (i + 1);
                xs[i] = MARGEN_IZQ + (int)((i + 0.5) * anchoPunto);
                ys[i] = MARGEN_ARR + altoGraf - (int)(util * altoGraf);
            }
            for (int i = 1; i < visible.size(); i++) {
                g2.drawLine(xs[i-1], ys[i-1], xs[i], ys[i]);
            }

            // Labels de ciclos en el eje X (cada ~50px)
            g2.setColor(Color.DARK_GRAY);
            g2.setFont(new Font("Dialog", Font.PLAIN, 9));
            int paso = Math.max(1, visible.size() / 8);
            for (int i = 0; i < visible.size(); i += paso) {
                int x = MARGEN_IZQ + (int)(i * anchoPunto);
                g2.drawLine(x, MARGEN_ARR + altoGraf, x, MARGEN_ARR + altoGraf + 4);
                g2.drawString(String.valueOf(ciclos.get(i)),
                    x - 5, MARGEN_ARR + altoGraf + 14);
            }

            // Borde del área
            g2.setColor(Color.GRAY);
            g2.setStroke(new BasicStroke(1f));
            g2.drawRect(MARGEN_IZQ, MARGEN_ARR, anchoGraf, altoGraf);
        }
    }
}
