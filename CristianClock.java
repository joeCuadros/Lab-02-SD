import java.util.ArrayList;
import java.util.List;

public class CristianClock {
    // escape de colores
    public static final String RESET = "\033[0m"; 
    public static final String GREEN = "\033[32m";
    public static final String RED = "\033[31m";

    public static void main(String[] args) {
        // arraylist para los hilos
        List<Thread> threads = new ArrayList<>();
        Server server = new Server();
        // hilo del servidor con funcion lambda
        Thread threadServer = new Thread(() -> {
            for (int j = 0; j < 3; j++) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                server.tick();
            }
            System.out.println(GREEN + "> (Termino) Server tiene " + server.getTick() + RESET);
        });
        // agregar el hilo del servidor al arraylist
        threads.add(threadServer);
        threadServer.start();
        // hilos de los nodos
        for (int i = 0; i < 3; i++) {
            // uso de la funcion lambda
            Thread thread = new Thread(() -> {
                List<Thread> threadsInternos = new ArrayList<>();
                //comportamiento de cada hilo
                Nodo n = new Nodo();
                // hilo de para el tick
                Thread threadTick = new Thread(() -> {
                    for (int j = 0; j < 3; j++) {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        n.tick();
                    }
                });
                // hilo para el sincronizacion
                Thread threadSincrozado = new Thread(() -> {
                    // mandar actualizacion con retraso
                    int t_inicial = n.getTick();
                    try {
                        Thread.sleep((long) (500 + Math.random() * 500));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    int timeServer = server.getTick(); //obtener despues de la espera
                    n.bloquearTicks();
                    int t_llegada = n.getTick();
                    int delay = (t_llegada - t_inicial)/2; //obtener tiempo aprox que demoro el mensaje
                    n.update(timeServer + delay);              
                });
                //ejecutar los hilos
                threadsInternos.add(threadTick);
                threadsInternos.add(threadSincrozado);
                threadTick.start(); 
                threadSincrozado.start(); 

                for (Thread threadInterno : threadsInternos) {
                    try {
                        threadInterno.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                n.getTick();
                System.out.println(RED +"(Termino) Thread "+ Thread.currentThread().threadId() 
                    + " tiene " + n.getTick() + RESET);
            });
            // agregar los hilos al arraylist
            threads.add(thread);
            thread.start();
        }
        // espera que termine lo hilos
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

class Nodo {
    private int clock;
    private long thread;
    private boolean sincronizado = false;
    // proteccion de condicion de carrera
    private boolean semaforo = true;

    public Nodo(){
        this.clock = (int) (Math.random() * 6); //empienza con un reloj desordenado
        this.thread = Thread.currentThread().threadId();
        System.out.println("(Inicio) Thread "+ this.thread+ " tiene " + this.clock);
    }
    public synchronized void tick(){
        while (!semaforo) {
            try {
                wait(); // espera hasta que esté permitido
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        this.clock++;
        if (this.sincronizado){
            System.out.println("(S) Thread " + this.thread + " tiene " + this.clock);
        }else {
            System.out.println("Thread " + this.thread + " tiene " + this.clock);
        }
    }
    public synchronized int getTick(){
        return this.clock;
    }
    public synchronized void update(int receivedTime){
        System.out.println(CristianClock.GREEN+ "? Thread " + this.thread + 
            " actualiza de " + this.clock + " a " + receivedTime+CristianClock.RESET);
        this.clock = receivedTime;
        this.sincronizado = true;
        this.semaforo = true;
        notify();
    }
    // metodo para bloquear
    public synchronized void bloquearTicks() {
        semaforo = false;
    }
}

class Server {
    private int clock;
    public Server(){
        this.clock = 3;
        System.out.println(CristianClock.GREEN+ "> (Inicio) Server tiene " + this.clock+CristianClock.RESET);
    }
    public synchronized void tick(){
        this.clock++;
        System.out.println(CristianClock.GREEN+"> Server tiene " + this.clock+CristianClock.RESET);
    }
    public synchronized int getTick(){
        return this.clock;
    }
}