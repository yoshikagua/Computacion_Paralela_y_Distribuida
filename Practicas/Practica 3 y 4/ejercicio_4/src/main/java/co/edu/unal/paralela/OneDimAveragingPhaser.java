package co.edu.unal.paralela;

import java.util.concurrent.Phaser;

/**
 * Clase que envuelve para implementar promedio iterativo usando
 * phasers de java.
 */
public final class OneDimAveragingPhaser {
    /**
     * Constructor por defecto.
     */
    private OneDimAveragingPhaser() {
    }

    /**
     * Implementación secuencial de un promedio iteractivo unidimensional.
     *
     * @param iterations El número de iteraciones que deben ser ejecutadas
     * @param myNew Un arreglo 'double' que inicia como el arreglo de salida
     * @param myVal Un arreglo 'double' que contiene la entrada inicial
     *        del problema del promedio iterativo 
     * @param n El tamaño de este problema
     */
    public static void runSequential(final int iterations, final double[] myNew,
            final double[] myVal, final int n) {
        double[] next = myNew;
        double[] curr = myVal;

        for (int iter = 0; iter < iterations; iter++) {
            for (int j = 1; j <= n; j++) {
                next[j] = (curr[j - 1] + curr[j + 1]) / 2.0;
            }
            double[] tmp = curr;
            curr = next;
            next = tmp;
        }
    }

    /**
     * Un ejemplo de implmentación paralela de promedio iterativo unidimiensional
     * que utiliza phasers como una barrera simple (arriveAndAwaitAdvance).
     *
     * @param iterations El número de iteraciones que deben ser ejecutadas
     * @param myNew Un arreglo 'double' que inicia como el arreglo de salida
     * @param myVal Un arreglo 'double' que contiene la entrada inicial
     *        del problema del promedio iterativo 
     * @param tasks El número de hilos/tareas para procesar 
     */
    public static void runParallelBarrier(final int iterations,
            final double[] myNew, final double[] myVal, final int n,
            final int tasks) {
        Phaser ph = new Phaser(0);
        ph.bulkRegister(tasks);

        Thread[] threads = new Thread[tasks];

        for (int ii = 0; ii < tasks; ii++) {
            final int i = ii;

            threads[ii] = new Thread(() -> {
                double[] threadPrivateMyVal = myVal;
                double[] threadPrivateMyNew = myNew;

                for (int iter = 0; iter < iterations; iter++) {
                    final int left = i * (n / tasks) + 1;
                    final int right = (i + 1) * (n / tasks);

                    for (int j = left; j <= right; j++) {
                        threadPrivateMyNew[j] = (threadPrivateMyVal[j - 1]
                            + threadPrivateMyVal[j + 1]) / 2.0;
                    }
                    ph.arriveAndAwaitAdvance();

                    double[] temp = threadPrivateMyNew;
                    threadPrivateMyNew = threadPrivateMyVal;
                    threadPrivateMyVal = temp;
                }
            });
            threads[ii].start();
        }

        for (int ii = 0; ii < tasks; ii++) {
            try {
                threads[ii].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Un ejemplo de implementación paralela de promedio iterativo unidimensional
     * que utiliza los APIs phasers.arrive y Phaser.awaitAdvance para traslapar 
     * la computación con "barrier completion".
     *
     * @param iterations El número de iteraciones que deben ser ejecutadas
     * @param myNew Un arreglo 'double' que inicia como el arreglo de salida
     * @param myVal Un arreglo 'double' que contiene la entrada inicial
     *        del problema del promedio iterativo 
     * @param tasks El número de hilos/tareas para procesar 
     */
    public static void runParallelFuzzyBarrier(final int iterations,
            final double[] myNew, final double[] myVal, final int n,
            final int tasks) {

        // Cada tarea tendrá su propio Phaser con 1 parte registrada (ella misma)
        Phaser[] phs = new Phaser[tasks];
        for (int i = 0; i < phs.length; i++) {
            phs[i] = new Phaser(1);
        }

        Thread[] threads = new Thread[tasks];

        for (int ii = 0; ii < tasks; ii++) {
            final int i = ii;

            threads[ii] = new Thread(() -> {
                double[] threadPrivateMyVal = myVal;
                double[] threadPrivateMyNew = myNew;

                for (int iter = 0; iter < iterations; iter++) {
                    final int left = i * (n / tasks) + 1;
                    final int right = (i + 1) * (n / tasks);

                    // Protección en caso de que el tamaño del problema sea menor que las tareas
                    if (left <= right) {
                        // 1. Calcular Frontera Izquierda
                        threadPrivateMyNew[left] = (threadPrivateMyVal[left - 1]
                                + threadPrivateMyVal[left + 1]) / 2.0;
                        
                        // 2. Calcular Frontera Derecha (si aplica)
                        if (right > left) {
                            threadPrivateMyNew[right] = (threadPrivateMyVal[right - 1]
                                    + threadPrivateMyVal[right + 1]) / 2.0;
                        }
                    }

                    // 3. SEÑALIZACIÓN: Avisamos a los vecinos que nuestras fronteras están listas
                    // Esto incrementa la fase de nuestro phaser dinámicamente de 'iter' a 'iter + 1'
                    phs[i].arrive();

                    // 4. TRABAJO ÚTIL (Fuzzy Región): Calculamos el interior mientras la barrera se resuelve
                    if (left <= right) {
                        for (int j = left + 1; j <= right - 1; j++) {
                            threadPrivateMyNew[j] = (threadPrivateMyVal[j - 1]
                                    + threadPrivateMyVal[j + 1]) / 2.0;
                        }
                    }

                    // 5. SINCRONIZACIÓN: Esperamos a que los vecinos hayan completado sus fases correspondientes
                    // awaitAdvance(iter) bloqueará solo si el phaser del vecino sigue en la fase 'iter'
                    if (i - 1 >= 0) {
                        phs[i - 1].awaitAdvance(iter);
                    }
                    if (i + 1 < tasks) {
                        phs[i + 1].awaitAdvance(iter);
                    }

                    // Intercambio seguro de punteros para la siguiente iteración
                    double[] temp = threadPrivateMyNew;
                    threadPrivateMyNew = threadPrivateMyVal;
                    threadPrivateMyVal = temp;
                }
            });
            threads[ii].start();
        }

        // Esperar a que todos los hilos terminen la ejecución global
        for (int ii = 0; ii < tasks; ii++) {
            try {
                threads[ii].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
