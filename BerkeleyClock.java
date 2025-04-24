import java.util.ArrayList;
import java.util.List;

public class BerkeleyClock {
    // escape de colores
    public static final String RESET = "\033[0m"; 
    public static final String GREEN = "\033[32m";
    public static final String RED = "\033[31m";

    public static void main(String[] args) {
        // arraylist para los hilos
        List<Thread> threads = new ArrayList<>();
        NodoCoordinador coordinador = new NodoCoordinador();
        List<Nodo> nodos = new ArrayList<>();
        // hilo del coordinador con funcion lambda
        Thread threadCoordinador = new Thread(() -> {
            List<Thread> threadsInterno = new ArrayList<>();
                // hilo de ticks
                Thread threadTick = new Thread(() -> {
                    for (int j = 0; j < 3; j++) {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        coordinador.tick();
                    }
                });
                // hilo para el sincronizacion
                Thread threadSincrozado = new Thread(() -> {
                    List<Integer> timeNodos = new ArrayList<>();
                    try {
                        Thread.sleep((long) (500 + Math.random() * 500));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    // extraer datos
                    for (Nodo nodo : nodos) {
                        timeNodos.add(nodo.getTick());
                    }  
                    coordinador.bloquearTicks();
                    coordinador.promedio(timeNodos);
                    coordinador.update(coordinador.getPromedio() - coordinador.getTick());
                });
                // Ejecutar lo hilos
                threadsInterno.add(threadTick);
                threadsInterno.add(threadSincrozado);
                threadTick.start();
                threadSincrozado.start();
                // espera que termine lo hilos internos
                for (Thread threadInt : threadsInterno) {
                    try {
                        threadInt.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                System.out.println(RED +"(Termino) Coordinador "+ Thread.currentThread().threadId() 
                    + " tiene " + coordinador.getTick() + RESET); 
        });
        // agregar el hilo del servidor al arraylist
        threads.add(threadCoordinador);
        threadCoordinador.start();
        // hilos de los nodos
        for (int i = 0; i < 3; i++) {
            // guardar los nodos
            Nodo n = new Nodo();
            nodos.add(n);

            // uso de la funcion lambda
            Thread thread = new Thread(() -> {
                List<Thread> threadsInterno = new ArrayList<>();
                // hilo de ticks
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
                    try {
                        Thread.sleep((long) (Math.random() * 1000));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    coordinador.esperarPromedio(); //esperando
                    n.bloquearTicks();
                    n.update(coordinador.getPromedio() - n.getTick());
                });
                // Ejecutar lo hilos
                threadsInterno.add(threadTick);
                threadsInterno.add(threadSincrozado);
                threadTick.start();
                threadSincrozado.start();
                // espera que termine lo hilos internos
                for (Thread threadInt : threadsInterno) {
                    try {
                        threadInt.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                System.out.println(RED +"(Termino) Nodo "+ Thread.currentThread().threadId() 
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
    protected long thread;
    protected String tipo;
    private boolean sincronizado = false;
    // proteccion de condicion de carrera
    private boolean semaforo = true;

    public Nodo(){
        this.clock = (int) (Math.random() * 11); //empienza con un reloj desordenado
        this.thread = Thread.currentThread().threadId();
        this.tipo = "Nodo";
        System.out.println("(Inicio) "+this.tipo + " " + this.thread+ " tiene " + this.clock);
    }
    public synchronized void tick(){
        while (!this.semaforo) {
            try {
                wait(); // espera hasta que esté permitido
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        this.clock++;
        if (this.sincronizado){
            System.out.println("(S) "+ this.tipo + " " + this.thread + " tiene " + this.clock);
        }else {
            System.out.println(this.tipo + " " + this.thread + " tiene " + this.clock);
        }
    }
    public synchronized int getTick(){
        return this.clock;
    }
    public synchronized void update(int ajuste){
        System.out.println(BerkeleyClock.GREEN+"("+ajuste+") " + this.tipo + " " + this.thread
            + " actualiza de " + this.clock + " a " + (this.clock + ajuste)+BerkeleyClock.RESET);
        this.clock = this.clock + ajuste;
        this.sincronizado = true;
        this.semaforo = true;
        notifyAll();
    }
    // metodo para bloquear
    public synchronized void bloquearTicks() {
        this.semaforo = false;
    }
}

class NodoCoordinador extends Nodo{
    private int promedio;
    private boolean semaforo2 = false;

    public NodoCoordinador(){
        super();
        this.tipo = "Coordinador";
        System.out.println("Coordinador nombrado "+this.thread);
    }
    public synchronized void promedio(List<Integer> tiempos){
        int suma = this.getTick();
        for (int i = 0; i < tiempos.size(); i++) {
            suma += tiempos.get(i);
        }
        this.promedio = (int)(suma / (tiempos.size() + 1)); // +1 por el nodo coordinador
        System.out.println(BerkeleyClock.GREEN+"(Coordinador) Saco el promedio "
            + this.promedio +BerkeleyClock.RESET);
        //semaforo para el promedio
        this.semaforo2 = true;
        notify();
    }
    public synchronized int getPromedio(){
        return this.promedio;
    }
    public synchronized boolean getSemaforo(){
        return this.semaforo2;
    }
    public synchronized void esperarPromedio() {
        while (!this.semaforo2) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}