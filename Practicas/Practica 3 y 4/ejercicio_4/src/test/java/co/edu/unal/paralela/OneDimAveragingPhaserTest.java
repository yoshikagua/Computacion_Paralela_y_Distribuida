package co.edu.unal.paralela;

import java.util.concurrent.Phaser;
import junit.framework.TestCase;

public class OneDimAveragingPhaserTest extends TestCase {
    // Número de veces para repetir cada test, para lograr resultados consistentes en el tiempo.
    final static private int niterations = 12000;

    private static int getNCores() {
        String ncoresStr = System.getenv("COURSERA_GRADER_NCORES");
        if (ncoresStr == null) {
            return Runtime.getRuntime().availableProcessors();
        } else {
            return Integer.parseInt(ncoresStr);
        }
    }

    private double[] createArray(final int N) {
        final double[] input = new double[N + 2];
        input[N + 1] = 1.0;
        return input;
    }

    /**
     * Una implementación de referencia de runSequential, en caso de que se modifique accidentalmente el código fuente.
     */
    public void runSequential(final int iterations, double[] myNew, double[] myVal, final int n) {
        for (int iter = 0; iter < iterations; iter++) {
            for (int j = 1; j <= n; j++) {
                myNew[j] = (myVal[j - 1] + myVal[j + 1]) / 2.0;
            }
            double[] tmp = myNew;
            myNew = myVal;
            myVal = tmp;
        }
    }

    private static void runParallelBarrier(final int iterations, final double[] myNew, final double[] myVal,
            final int n, final int tasks) {
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
                        threadPrivateMyNew[j] = (threadPrivateMyVal[j - 1] + threadPrivateMyVal[j + 1]) / 2.0;
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

    private void checkResult(final double[] ref, final double[] output) {
        for (int i = 0; i < ref.length; i++) {
            String msg = "Mismatch on output at element " + i;
            assertEquals(msg, ref[i], output[i]);
        }
    }

    private static class ParResult {
        double elapsedMillis;
        double expectedSpeedup;
        double actualSpeedup;
        int ntasks;
        int ncores;
        boolean passed;
    }

    /**
     * Función helper para probar la implementación paralela.
     *
     * @param N El tamaño del arreglo para hacer la prueba
     * @param ntasks El número de hilos/tareas concurrentes
     * @return El objeto ParResult con las métricas obtenidas
     */
    private ParResult parTestHelper(final int N, final int ntasks) {
        // Crea una entrada de forma aleatoria
        double[] myNew = createArray(N);
        double[] myVal = createArray(N);
        final double[] myNewRef = createArray(N);
        final double[] myValRef = createArray(N);

        final long barrierStartTime = System.currentTimeMillis();
        runParallelBarrier(niterations, myNew, myVal, N, ntasks);
        final long barrierEndTime = System.currentTimeMillis();

        final long fuzzyStartTime = System.currentTimeMillis();
        OneDimAveragingPhaser.runParallelFuzzyBarrier(niterations, myNewRef, myValRef, N, ntasks);
        final long fuzzyEndTime = System.currentTimeMillis();

        if (niterations % 2 == 0) {
            checkResult(myNewRef, myNew);
        } else {
            checkResult(myValRef, myVal);
        }

        final long barrierTime = barrierEndTime - barrierStartTime;
        final long fuzzyTime = fuzzyEndTime - fuzzyStartTime;

        final double actualSpeedup = (double) barrierTime / (double) fuzzyTime;
        final double expectedSpeedup = 1.1;
        final boolean passed = actualSpeedup >= expectedSpeedup;

        ParResult res = new ParResult();
        res.elapsedMillis = (double) fuzzyTime; 
        res.expectedSpeedup = expectedSpeedup;
        res.actualSpeedup = actualSpeedup;
        res.ntasks = ntasks;
        res.ncores = getNCores();
        res.passed = passed;
        
        return res;
    }

    /**
     * Prueba sobre una entrada de gran tamaño imprimiendo detalles técnicos completos en consola.
     */
    public void testFuzzyBarrier() {
        final int ntasks = getNCores() * 16;
        final ParResult r = parTestHelper(4 * 1024 * 1024, ntasks);
        final String testName = "testFuzzyBarrier";
        
        System.out.printf("[%s] %-30s   | tiempo= %9.3f ms | esperado= %6.3fx | real= %6.3fx | tareas= %d | nucleos= %d%s%n",
                r.passed ? "PASS" : "FAIL",
                testName,
                r.elapsedMillis,
                r.expectedSpeedup,
                r.actualSpeedup,
                r.ntasks,
                r.ncores,
                r.passed ? "" : " | no alcanza el minimo esperado");

        final String errMsg = String.format("It was expected that the fuzzy barrier parallel implementation would " +
                "run %fx faster than the barrier implementation, but it only achieved %fx speedup", r.expectedSpeedup, r.actualSpeedup);
        
        assertTrue(errMsg, r.passed);
    }
}