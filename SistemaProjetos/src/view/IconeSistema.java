package view;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Ícones vetoriais leves para a interface Swing.
 * Os desenhos seguem a mesma família visual utilizada na versão Web
 * (Bootstrap Icons), sem exigir bibliotecas externas no projeto Java.
 */
public final class IconeSistema implements Icon {
    private final String nome;
    private final int tamanho;
    private final Color cor;

    public IconeSistema(String nome, int tamanho, Color cor) {
        this.nome = nome == null ? "" : nome;
        this.tamanho = Math.max(8, tamanho);
        this.cor = cor == null ? Color.WHITE : cor;
    }

    @Override public int getIconWidth() { return tamanho; }
    @Override public int getIconHeight() { return tamanho; }

    @Override
    public void paintIcon(Component componente, Graphics graphics, int x, int y) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.translate(x, y);
            double escala = tamanho / 24.0;
            g.scale(escala, escala);
            g.setColor(cor);
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            desenhar(g);
        } finally {
            g.dispose();
        }
    }

    private void desenhar(Graphics2D g) {
        Path2D p;
        switch (nome) {
            case "layers":
                p = new Path2D.Double();
                p.moveTo(12, 2.5); p.lineTo(22, 8); p.lineTo(12, 13.5); p.lineTo(2, 8); p.closePath();
                g.fill(p);
                p = new Path2D.Double();
                p.moveTo(3, 12); p.lineTo(12, 17); p.lineTo(21, 12);
                g.draw(p);
                p = new Path2D.Double();
                p.moveTo(3, 16); p.lineTo(12, 21); p.lineTo(21, 16);
                g.draw(p);
                break;
            case "home":
                p = new Path2D.Double();
                p.moveTo(2.5, 11); p.lineTo(12, 3); p.lineTo(21.5, 11);
                p.lineTo(19, 11); p.lineTo(19, 21); p.lineTo(5, 21); p.lineTo(5, 11); p.closePath();
                g.fill(p);
                g.setColor(new Color(13, 48, 82));
                g.fill(new RoundRectangle2D.Double(10, 14, 4, 7, 1, 1));
                break;
            case "folder":
                p = new Path2D.Double();
                p.moveTo(2, 6); p.lineTo(9, 6); p.lineTo(11, 8); p.lineTo(22, 8);
                p.lineTo(22, 19); p.quadTo(22, 21, 20, 21); p.lineTo(4, 21);
                p.quadTo(2, 21, 2, 19); p.closePath();
                g.fill(p);
                break;
            case "grid":
                g.fill(new RoundRectangle2D.Double(3, 3, 7, 7, 1.2, 1.2));
                g.fill(new RoundRectangle2D.Double(14, 3, 7, 7, 1.2, 1.2));
                g.fill(new RoundRectangle2D.Double(3, 14, 7, 7, 1.2, 1.2));
                g.fill(new RoundRectangle2D.Double(14, 14, 7, 7, 1.2, 1.2));
                break;
            case "chart":
                g.fill(new RoundRectangle2D.Double(3, 12, 4, 9, 1, 1));
                g.fill(new RoundRectangle2D.Double(10, 7, 4, 14, 1, 1));
                g.fill(new RoundRectangle2D.Double(17, 3, 4, 18, 1, 1));
                break;
            case "gear":
                g.draw(new Ellipse2D.Double(7, 7, 10, 10));
                for (int a = 0; a < 8; a++) {
                    double angulo = Math.PI * a / 4.0;
                    double x1 = 12 + Math.cos(angulo) * 7;
                    double y1 = 12 + Math.sin(angulo) * 7;
                    double x2 = 12 + Math.cos(angulo) * 10;
                    double y2 = 12 + Math.sin(angulo) * 10;
                    g.draw(new java.awt.geom.Line2D.Double(x1, y1, x2, y2));
                }
                break;
            case "logout":
                g.draw(new RoundRectangle2D.Double(3, 3, 11, 18, 1.5, 1.5));
                p = new Path2D.Double();
                p.moveTo(10, 12); p.lineTo(22, 12); p.moveTo(18, 8); p.lineTo(22, 12); p.lineTo(18, 16);
                g.draw(p);
                break;
            case "check":
                p = new Path2D.Double();
                p.moveTo(4, 12); p.lineTo(9.5, 17.5); p.lineTo(20, 6.5);
                g.setStroke(new BasicStroke(2.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(p);
                break;
            case "clock":
                g.draw(new Ellipse2D.Double(3, 3, 18, 18));
                p = new Path2D.Double();
                p.moveTo(12, 6); p.lineTo(12, 12); p.lineTo(16, 14.5);
                g.draw(p);
                break;
            case "calendar":
                g.draw(new RoundRectangle2D.Double(3, 5, 18, 16, 2, 2));
                g.draw(new java.awt.geom.Line2D.Double(3, 10, 21, 10));
                g.draw(new java.awt.geom.Line2D.Double(8, 3, 8, 7));
                g.draw(new java.awt.geom.Line2D.Double(16, 3, 16, 7));
                g.fill(new Ellipse2D.Double(7, 13, 2, 2));
                g.fill(new Ellipse2D.Double(11, 13, 2, 2));
                g.fill(new Ellipse2D.Double(15, 13, 2, 2));
                break;
            case "search":
                g.draw(new Ellipse2D.Double(3, 3, 13, 13));
                g.draw(new java.awt.geom.Line2D.Double(14, 14, 21, 21));
                break;
            case "refresh":
                g.draw(new Arc2D.Double(4, 4, 16, 16, 45, 290, Arc2D.OPEN));
                p = new Path2D.Double();
                p.moveTo(17, 3); p.lineTo(21, 4); p.lineTo(20, 8);
                g.fill(p);
                break;
            case "pencil":
                g.setStroke(new BasicStroke(2.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                p = new Path2D.Double();
                p.moveTo(4, 17.5); p.lineTo(16.5, 5); p.quadTo(18, 3.5, 19.5, 5);
                p.lineTo(21, 6.5); p.quadTo(22.5, 8, 21, 9.5); p.lineTo(8.5, 22); p.lineTo(3, 23); p.closePath();
                g.draw(p);
                break;
            case "trash":
                g.draw(new RoundRectangle2D.Double(6, 7, 12, 14, 1.5, 1.5));
                g.draw(new java.awt.geom.Line2D.Double(4, 5, 20, 5));
                g.draw(new java.awt.geom.Line2D.Double(9, 3, 15, 3));
                g.draw(new java.awt.geom.Line2D.Double(10, 10, 10, 18));
                g.draw(new java.awt.geom.Line2D.Double(14, 10, 14, 18));
                break;
            case "save":
                g.draw(new RoundRectangle2D.Double(3, 3, 18, 18, 2, 2));
                g.draw(new java.awt.geom.Line2D.Double(7, 3, 7, 9));
                g.draw(new java.awt.geom.Line2D.Double(17, 3, 17, 9));
                g.draw(new RoundRectangle2D.Double(7, 13, 10, 7, 1, 1));
                break;
            case "eraser":
                p = new Path2D.Double();
                p.moveTo(8, 3); p.lineTo(21, 16); p.lineTo(15, 22); p.lineTo(2, 9); p.closePath();
                g.draw(p);
                g.draw(new java.awt.geom.Line2D.Double(8, 15, 14, 9));
                g.draw(new java.awt.geom.Line2D.Double(14, 22, 22, 22));
                break;
            case "sun":
                g.draw(new Ellipse2D.Double(8, 8, 8, 8));
                for (int a = 0; a < 8; a++) {
                    double angulo = Math.PI * a / 4.0;
                    g.draw(new java.awt.geom.Line2D.Double(
                            12 + Math.cos(angulo) * 7, 12 + Math.sin(angulo) * 7,
                            12 + Math.cos(angulo) * 10, 12 + Math.sin(angulo) * 10));
                }
                break;
            case "moon":
                g.draw(new Arc2D.Double(4, 3, 17, 18, 55, 250, Arc2D.OPEN));
                g.draw(new java.awt.geom.Line2D.Double(14, 3, 18, 5));
                g.draw(new java.awt.geom.Line2D.Double(18, 5, 20, 10));
                break;
            case "plus":
                g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new java.awt.geom.Line2D.Double(12, 4, 12, 20));
                g.draw(new java.awt.geom.Line2D.Double(4, 12, 20, 12));
                break;
            case "user":
                g.draw(new Ellipse2D.Double(8, 3, 8, 8));
                g.draw(new Arc2D.Double(3, 12, 18, 12, 0, 180, Arc2D.OPEN));
                break;
            case "monitor":
            case "desktop":
                g.draw(new RoundRectangle2D.Double(2.5, 3.5, 19, 14, 1.5, 1.5));
                g.draw(new java.awt.geom.Line2D.Double(12, 17.5, 12, 21));
                g.draw(new java.awt.geom.Line2D.Double(7, 21, 17, 21));
                break;
            case "mobile":
                g.draw(new RoundRectangle2D.Double(6.5, 1.5, 11, 21, 2, 2));
                g.draw(new java.awt.geom.Line2D.Double(10, 4, 14, 4));
                g.fill(new Ellipse2D.Double(11, 19, 2, 2));
                break;
            case "globe":
                g.draw(new Ellipse2D.Double(2.5, 2.5, 19, 19));
                g.draw(new Arc2D.Double(7, 2.5, 10, 19, 90, 180, Arc2D.OPEN));
                g.draw(new Arc2D.Double(7, 2.5, 10, 19, 270, 180, Arc2D.OPEN));
                g.draw(new java.awt.geom.Line2D.Double(3, 12, 21, 12));
                g.draw(new Arc2D.Double(3, 7, 18, 10, 0, 180, Arc2D.OPEN));
                g.draw(new Arc2D.Double(3, 7, 18, 10, 180, 180, Arc2D.OPEN));
                break;
            case "download":
                p = new Path2D.Double();
                p.moveTo(12, 3); p.lineTo(12, 15); p.moveTo(7, 10); p.lineTo(12, 15); p.lineTo(17, 10);
                g.draw(p);
                g.draw(new java.awt.geom.Line2D.Double(4, 19, 20, 19));
                g.draw(new java.awt.geom.Line2D.Double(4, 19, 4, 21));
                g.draw(new java.awt.geom.Line2D.Double(20, 19, 20, 21));
                break;
            case "palette":
                g.draw(new Ellipse2D.Double(2.5, 3, 19, 18));
                g.fill(new Ellipse2D.Double(7, 7, 2.5, 2.5));
                g.fill(new Ellipse2D.Double(12, 5.5, 2.5, 2.5));
                g.fill(new Ellipse2D.Double(16, 9, 2.5, 2.5));
                g.fill(new Ellipse2D.Double(8, 14, 2.5, 2.5));
                break;
            case "sliders":
                g.draw(new java.awt.geom.Line2D.Double(3, 6, 21, 6));
                g.draw(new java.awt.geom.Line2D.Double(3, 12, 21, 12));
                g.draw(new java.awt.geom.Line2D.Double(3, 18, 21, 18));
                g.setColor(Color.WHITE); g.fill(new Ellipse2D.Double(7, 3.5, 5, 5));
                g.setColor(cor); g.draw(new Ellipse2D.Double(7, 3.5, 5, 5));
                g.setColor(Color.WHITE); g.fill(new Ellipse2D.Double(14, 9.5, 5, 5));
                g.setColor(cor); g.draw(new Ellipse2D.Double(14, 9.5, 5, 5));
                g.setColor(Color.WHITE); g.fill(new Ellipse2D.Double(5, 15.5, 5, 5));
                g.setColor(cor); g.draw(new Ellipse2D.Double(5, 15.5, 5, 5));
                break;
            case "server":
                for (int y : new int[]{3, 10, 17}) {
                    g.draw(new RoundRectangle2D.Double(3, y, 18, 5, 1, 1));
                    g.fill(new Ellipse2D.Double(6, y + 1.5, 2, 2));
                    g.draw(new java.awt.geom.Line2D.Double(12, y + 2.5, 18, y + 2.5));
                }
                break;
            case "database":
                g.draw(new Ellipse2D.Double(4, 2.5, 16, 6));
                g.draw(new java.awt.geom.Line2D.Double(4, 5.5, 4, 18));
                g.draw(new java.awt.geom.Line2D.Double(20, 5.5, 20, 18));
                g.draw(new Arc2D.Double(4, 11, 16, 6, 180, 180, Arc2D.OPEN));
                g.draw(new Arc2D.Double(4, 15, 16, 6, 180, 180, Arc2D.OPEN));
                g.draw(new Arc2D.Double(4, 2.5, 16, 6, 0, -180, Arc2D.OPEN));
                break;
            default:
                g.draw(new Ellipse2D.Double(4, 4, 16, 16));
        }
    }
}
