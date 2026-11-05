import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Random;


//Hecho por Sergio García Ramiro adaptado del original
class BotonJuego extends JButton {
    private Color colorBase;
    private Color colorHover;

    public BotonJuego(String texto, Color fondo) {
        super(texto);
        this.colorBase = fondo;
        this.colorHover = fondo.brighter();

        setFont(new Font("SansSerif", Font.BOLD, 14));
        setBackground(colorBase);
        setForeground(Color.WHITE);
        setFocusPainted(false);
        setBorderPainted(false);
        setOpaque(true);
        setCursor(new Cursor(Cursor.HAND_CURSOR));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                setBackground(colorHover);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setBackground(colorBase);
            }
        });
    }

    public void setColorFondo(Color c) {
        this.colorBase = c;
        this.colorHover = c.brighter();
        setBackground(c);
    }
}

class ElementoJuego extends JLabel {
    protected int velocidad;

    public ElementoJuego(String rutaImagen, Color colorFallback, int x, int y, int ancho, int alto, int vel) {
        super();
        this.velocidad = vel;
        setBounds(x, y, ancho, alto);

        boolean cargada = false;
        File f = new File(rutaImagen);
        if (f.exists()) {
            ImageIcon icono = new ImageIcon(f.getAbsolutePath());
            if (icono.getIconWidth() > 0) {
                Image img = icono.getImage().getScaledInstance(ancho, alto, Image.SCALE_SMOOTH);
                setIcon(new ImageIcon(img));
                cargada = true;
            }
        }

        if (!cargada) {
            setOpaque(true);
            setBackground(colorFallback);
            setBorder(BorderFactory.createLineBorder(Color.WHITE, 1));
        }
    }

    public void mover(int dx, int dy) {
        setLocation(getX() + dx, getY() + dy);
    }

    public boolean colisionaCon(ElementoJuego otro) {
        return getBounds().intersects(otro.getBounds());
    }
}

class PalaJugador extends ElementoJuego {
    public PalaJugador(int x, int y, int ancho, int alto) {
        super("mbappe.jpg", new Color(30, 144, 255), x, y, ancho, alto, 24);
    }

    public void moverIzquierda() {
        if (getX() - velocidad >= 0) {
            mover(-velocidad, 0);
        } else {
            setLocation(0, getY());
        }
    }

    public void moverDerecha(int anchoEscenario) {
        if (getX() + getWidth() + velocidad <= anchoEscenario) {
            mover(velocidad, 0);
        } else {
            setLocation(anchoEscenario - getWidth(), getY());
        }
    }
}

class PalaRival extends ElementoJuego {
    public PalaRival(int x, int y, int ancho, int alto) {
        super("rival.jpg", new Color(220, 50, 50), x, y, ancho, alto, 4);
    }

    public void seguirPelota(int centroPelotaX, int anchoEscenario) {
        int centroRival = getX() + (getWidth() / 2);
        int distancia = centroPelotaX - centroRival;

        if (distancia > 6) {
            int nuevoX = Math.min(getX() + velocidad, anchoEscenario - getWidth());
            setLocation(nuevoX, getY());
        } else if (distancia < -6) {
            int nuevoX = Math.max(0, getX() - velocidad);
            setLocation(nuevoX, getY());
        }
    }
}

class Pelota extends ElementoJuego {
    private int dx = 4;
    private int dy = 4;

    public Pelota(int x, int y, int diametro) {
        super("pelota.png", new Color(255, 215, 0), x, y, diametro, diametro, 4);
    }

    public void actualizar(int anchoEscenario) {
        mover(dx, dy);

        if (getX() <= 0) {
            setLocation(0, getY());
            dx = Math.abs(dx);
        } else if (getX() + getWidth() >= anchoEscenario) {
            setLocation(anchoEscenario - getWidth(), getY());
            dx = -Math.abs(dx);
        }
    }

    public void rebotarHaciaArriba() {
        dy = -Math.abs(dy);
    }

    public void rebotarHaciaAbajo() {
        dy = Math.abs(dy);
    }

    public boolean pasoFondoJugador(int altoEscenario) {
        return getY() >= altoEscenario;
    }

    public boolean pasoFondoRival() {
        return getY() + getHeight() <= 0;
    }

    public void reiniciar(int x, int y, boolean sacaJugador) {
        setLocation(x, y);
        Random r = new Random();
        dx = (r.nextBoolean() ? 1 : -1) * 4;
        dy = sacaJugador ? -4 : 4;
    }

    public int getCentroX() {
        return getX() + (getWidth() / 2);
    }
}

public class PongBotones extends JFrame {

    private enum EstadoJuego { INICIO, JUGANDO, PAUSA, GAME_OVER, VICTORIA }
    private EstadoJuego estadoActual = EstadoJuego.INICIO;

    private final int ANCHO = 600;
    private final int ALTO = 440;

    private JPanel escenario;
    private PalaJugador jugador;
    private PalaRival rival;
    private Pelota pelota;

    private JLabel etiquetaMarcador;
    private JLabel etiquetaMensajeCentro;

    private BotonJuego btnIzquierda;
    private BotonJuego btnDerecha;
    private BotonJuego btnAccion;

    private int vidas = 3;
    private int puntosJugador = 0;
    private int puntosRival = 0;
    private final int PUNTOS_PARA_GANAR = 50;

    private Timer temporizador;

    public PongBotones() {
        setTitle("Pong 2D - Sergio García Ramiro");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        etiquetaMarcador = new JLabel("Vidas: 3    Tú: 0  -  Rival: 0    Estado: INICIO");
        etiquetaMarcador.setFont(new Font("SansSerif", Font.BOLD, 15));
        etiquetaMarcador.setForeground(Color.WHITE);
        etiquetaMarcador.setBackground(new Color(20, 20, 25));
        etiquetaMarcador.setOpaque(true);
        etiquetaMarcador.setHorizontalAlignment(SwingConstants.CENTER);
        etiquetaMarcador.setPreferredSize(new Dimension(ANCHO, 36));
        add(etiquetaMarcador, BorderLayout.NORTH);

        escenario = new JPanel(null);
        escenario.setPreferredSize(new Dimension(ANCHO, ALTO));
        escenario.setBackground(new Color(15, 15, 18));

        rival = new PalaRival(250, 20, 100, 24);
        jugador = new PalaJugador(250, 390, 100, 24);
        pelota = new Pelota(285, 200, 30);

        etiquetaMensajeCentro = new JLabel("PULSA 'JUGAR' PARA EMPEZAR", SwingConstants.CENTER);
        etiquetaMensajeCentro.setFont(new Font("SansSerif", Font.BOLD, 22));
        etiquetaMensajeCentro.setForeground(Color.YELLOW);
        etiquetaMensajeCentro.setBounds(50, 180, 500, 40);

        escenario.add(etiquetaMensajeCentro);
        escenario.add(rival);
        escenario.add(jugador);
        escenario.add(pelota);
        add(escenario, BorderLayout.CENTER);

        JPanel panelControles = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        panelControles.setBackground(new Color(30, 30, 35));

        btnIzquierda = new BotonJuego("◀ IZQUIERDA", new Color(40, 110, 200));
        btnAccion = new BotonJuego("JUGAR", new Color(40, 160, 80));
        btnDerecha = new BotonJuego("DERECHA ▶", new Color(40, 110, 200));

        btnIzquierda.addActionListener(e -> {
            if (estadoActual == EstadoJuego.JUGANDO) {
                jugador.moverIzquierda();
            }
        });

        btnDerecha.addActionListener(e -> {
            if (estadoActual == EstadoJuego.JUGANDO) {
                jugador.moverDerecha(escenario.getWidth());
            }
        });

        btnAccion.addActionListener(e -> gestionarBotonAccion());

        panelControles.add(btnIzquierda);
        panelControles.add(btnAccion);
        panelControles.add(btnDerecha);
        add(panelControles, BorderLayout.SOUTH);

        temporizador = new Timer(16, e -> bucleJuego());

        pack();
        setLocationRelativeTo(null);
        setResizable(false);
        setVisible(true);
    }

    private void gestionarBotonAccion() {
        switch (estadoActual) {
            case INICIO:
                iniciarPartida();
                break;
            case JUGANDO:
                pausarPartida();
                break;
            case PAUSA:
                reanudarPartida();
                break;
            case GAME_OVER:
            case VICTORIA:
                reiniciarPartida();
                break;
        }
    }

    private void iniciarPartida() {
        estadoActual = EstadoJuego.JUGANDO;
        etiquetaMensajeCentro.setVisible(false);
        btnAccion.setText("PAUSAR");
        btnAccion.setColorFondo(new Color(220, 140, 20));
        actualizarMarcador();
        temporizador.start();
    }

    private void pausarPartida() {
        estadoActual = EstadoJuego.PAUSA;
        temporizador.stop();
        etiquetaMensajeCentro.setText("PARTIDA PAUSADA");
        etiquetaMensajeCentro.setForeground(Color.CYAN);
        etiquetaMensajeCentro.setVisible(true);
        btnAccion.setText("REANUDAR");
        btnAccion.setColorFondo(new Color(40, 160, 80));
        actualizarMarcador();
    }

    private void reanudarPartida() {
        estadoActual = EstadoJuego.JUGANDO;
        etiquetaMensajeCentro.setVisible(false);
        btnAccion.setText("PAUSAR");
        btnAccion.setColorFondo(new Color(220, 140, 20));
        actualizarMarcador();
        temporizador.start();
    }

    private void reiniciarPartida() {
        vidas = 3;
        puntosJugador = 0;
        puntosRival = 0;
        jugador.setLocation(250, 390);
        rival.setLocation(250, 20);
        pelota.reiniciar(285, 200, true);
        iniciarPartida();
    }

    private void bucleJuego() {
        if (estadoActual != EstadoJuego.JUGANDO) return;

        pelota.actualizar(escenario.getWidth());
        rival.seguirPelota(pelota.getCentroX(), escenario.getWidth());

        if (pelota.colisionaCon(jugador)) {
            pelota.rebotarHaciaArriba();
            puntosJugador += 10;
            actualizarMarcador();

            if (puntosJugador >= PUNTOS_PARA_GANAR) {
                partidaGanada();
                return;
            }
        }

        if (pelota.colisionaCon(rival)) {
            pelota.rebotarHaciaAbajo();
        }

        if (pelota.pasoFondoRival()) {
            puntosJugador += 20;
            actualizarMarcador();
            if (puntosJugador >= PUNTOS_PARA_GANAR) {
                partidaGanada();
            } else {
                pelota.reiniciar(escenario.getWidth() / 2, escenario.getHeight() / 2, false);
            }
            return;
        }

        if (pelota.pasoFondoJugador(escenario.getHeight())) {
            vidas--;
            puntosRival += 10;
            actualizarMarcador();

            if (vidas <= 0) {
                gameOver();
            } else {
                pelota.reiniciar(escenario.getWidth() / 2, escenario.getHeight() / 2, true);
            }
        }
    }

    private void partidaGanada() {
        estadoActual = EstadoJuego.VICTORIA;
        temporizador.stop();
        etiquetaMensajeCentro.setText("¡VICTORIA! Puntos: " + puntosJugador);
        etiquetaMensajeCentro.setForeground(new Color(50, 220, 90));
        etiquetaMensajeCentro.setVisible(true);
        btnAccion.setText("REINTENTAR");
        btnAccion.setColorFondo(new Color(40, 160, 80));
        actualizarMarcador();
    }

    private void gameOver() {
        estadoActual = EstadoJuego.GAME_OVER;
        temporizador.stop();
        etiquetaMensajeCentro.setText("GAME OVER");
        etiquetaMensajeCentro.setForeground(new Color(240, 60, 60));
        etiquetaMensajeCentro.setVisible(true);
        btnAccion.setText("REINTENTAR");
        btnAccion.setColorFondo(new Color(200, 50, 50));
        actualizarMarcador();
    }

    private void actualizarMarcador() {
        etiquetaMarcador.setText("Vidas: " + vidas + "    Tú: " + puntosJugador + "  -  Rival: " + puntosRival + "    Estado: " + estadoActual);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PongBotones());
    }
}
