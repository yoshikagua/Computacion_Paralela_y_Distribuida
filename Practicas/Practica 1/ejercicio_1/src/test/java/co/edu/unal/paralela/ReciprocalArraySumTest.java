package co.edu.unal.paralela;

import java.util.Random;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class ReciprocalArraySumTest extends TestCase {
    // Número de veces que se debe repetir cada prueba para dar consistencia de los resultados de en el tiempo.
    final static private int REPEATS = 60;

    /**
     * Guarda un resumen compacto de una ejecución de prueba.
     */
    private static final class TestOutcome {
        private final String name;
        private final boolean passed;
        private final double expectedSpeedup;
        private final double actualSpeedup;
        private final double elapsedMillis;
        private final String reason;

        TestOutcome(final String setName, final boolean setPassed,
                final double setExpectedSpeedup, final double setActualSpeedup,
                final double setElapsedMillis, final String setReason) {
            this.name = setName;
            this.passed = setPassed;
            this.expectedSpeedup = setExpectedSpeedup;
            this.actualSpeedup = setActualSpeedup;
            this.elapsedMillis = setElapsedMillis;
            this.reason = setReason;
        }
    }

    /**
     * Resultado acumulado de todas las pruebas.
     */
    private static final java.util.List<TestOutcome> OUTCOMES = new java.util.ArrayList<TestOutcome>();

    /**
     * Fuerza un orden fijo de ejecución para los métodos de prueba.
     *
     * @return suite con el orden deseado
     */
    public static Test suite() {
        final TestSuite suite = new TestSuite();
        suite.addTest(new ReciprocalArraySumTest("testParSimpleTwoMillion"));
        suite.addTest(new ReciprocalArraySumTest("testParSimpleTwoHundredMillion"));
        suite.addTest(new ReciprocalArraySumTest("testParManyTaskTwoMillion"));
        suite.addTest(new ReciprocalArraySumTest("testParManyTaskTwoHundredMillion"));
        return suite;
    }

    /**
     * Constructor JUnit 3 para poder fijar el nombre del test dentro de suite().
     *
     * @param name nombre del método de prueba
     */
    public ReciprocalArraySumTest(final String name) {
        super(name);
    }

    /**
     * Constructor por defecto.
     */
    public ReciprocalArraySumTest() {
        super();
    }

    private static int getNCores() {
            return Runtime.getRuntime().availableProcessors();
    }

    /**
     * Crea un arreglo double[] de longitud N para utilizar como entrada para las pruebas.
     *
     * @param N Tamaño del arreglo a crear
     * @return Arreglo double de longitud N inicializado
     */
    private double[] createArray(final int N) {
        final double[] input = new double[N];
        final Random rand = new Random(314);

        for (int i = 0; i < N; i++) {
            input[i] = rand.nextInt(100);
            // No se permiten valores en cero en el arreglo de entrada para evitar la división por cero
            if (input[i] == 0.0) {
                i--;
            }
        }

        return input;
    }

    /**
     * Una implementación de referencia de seqArraysum, en caso de que alguno del archvo del código fuente principal sea modificado accidentalmente.
     *
     * @param input Enrada para calcular secuencialmente la suma de los recíprocos
     * @return Suma de los recíprocos de la entrada
     */
    private double seqArraySum(final double[] input) {
        double sum = 0;

        // Calcula la suma de los recíprocos de los elementos del arreglo
        for (int i = 0; i < input.length; i++) {
            sum += 1 / input[i];
        }

        return sum;
    }

    /**
     * Imprime un resumen legible del resultado de una prueba de speedup.
     *
     * @param testName Nombre de la prueba
     * @param expectedSpeedup Speedup mínimo esperado
     * @param actualSpeedup Speedup medido
     */
    private void recordSpeedupSummary(final String testName,
            final double expectedSpeedup, final double actualSpeedup,
            final double elapsedMillis, final int ntasks) {
        final boolean passed = actualSpeedup >= expectedSpeedup;
        final String reason = passed ? "" : "no alcanza el minimo esperado";
        OUTCOMES.add(new TestOutcome(testName, passed, expectedSpeedup,
            actualSpeedup, elapsedMillis, reason));
        System.out.printf("[%s] %-30s   | tiempo= %9.3f ms | esperado= %6.3fx | real= %6.3fx | tareas= %d%n",
            passed ? "PASS" : "FAIL",
            testName,
            elapsedMillis,
            expectedSpeedup,
            actualSpeedup,
            ntasks,
            passed ? "" : " | no alcanza el minimo esperado");
    }

    /**
     * Una función 'helper' para hacer las pruebas de la implementación de dos y la implementación de muchas tareas en paralelo.
     *
     * @param N Tamaño del arreglo utilizado para las pruebas
     * @param useManyTaskVersion Switch entre el código la versión de dos tareas en paralelo y la versión de muchas tareas en paralelo
     * @param ntasks Número de tareas a utilizar
     * @return La mejora en la rapidez (speedup) alcanzada, no todas las pruebas utilizan esta información
     */
    private double parTestHelper(final int N, final boolean useManyTaskVersion, final int ntasks) {
        // Crea un arreglo de entrada de manera aleatoria
        final double[] input = createArray(N);
        // Utilza una version secuencial para calcular el resultado correcto
        final double correct = seqArraySum(input);
        // Utiliza la implementación paralela para calcular el resultado
        double sum;
        if (useManyTaskVersion) {
            sum = ReciprocalArraySum.parManyTaskArraySum(input, ntasks);
        } else {
            assert ntasks == 2;
            sum = ReciprocalArraySum.parArraySum(input);
        }
        final double err = Math.abs(sum - correct);
        // Asegura que la salida esperada sea la calculada
        final String errMsg = String.format("No concuerda el resultado para N = %d, valor esperado = %f, valor calculado = %f, error " +
                "absoluto = %f", N, correct, sum, err);
        assertTrue(errMsg, err < 1E-2);

        /*
         * Ejecuta varias repeticiones de la versiones secuncial y paralela para obtener una medida más exacta del desempeño paralelo.
         */
        final long seqStartTime = System.nanoTime();
        for (int r = 0; r < REPEATS; r++) {
            seqArraySum(input);
        }
        final long seqEndTime = System.nanoTime();

        final long parStartTime = System.nanoTime();
        for (int r = 0; r < REPEATS; r++) {
            if (useManyTaskVersion) {
                ReciprocalArraySum.parManyTaskArraySum(input, ntasks);
            } else {
                assert ntasks == 2;
                ReciprocalArraySum.parArraySum(input);
            }
        }
        final long parEndTime = System.nanoTime();

        final double seqTime = (double)(seqEndTime - seqStartTime) / REPEATS;
        final double parTime = (double)(parEndTime - parStartTime) / REPEATS;

        return seqTime / parTime;
    }

    /**
     * Ejecuta una prueba de rendimiento y devuelve speedup con tiempo total de ejecución.
     *
     * @param N Tamaño del arreglo a probar
     * @param useManyTaskVersion Selecciona la versión de muchas tareas
     * @param ntasks Número de tareas a usar
     * @return resultado de la prueba
     */
    private TestOutcome runTestWithTiming(final int N,
            final boolean useManyTaskVersion, final int ntasks) {
        final long startTime = System.nanoTime();
        final double speedup = parTestHelper(N, useManyTaskVersion, ntasks);
        final long endTime = System.nanoTime();
        return new TestOutcome("", true, 0.0, speedup, (endTime - startTime) / 1_000_000.0, "");
    }

    /**
     * Prueba que la implementación de dos tareas en paralelo calcula correctamente los resultados para arreglos con un millón de elementos.
     */
    public void testParSimpleTwoMillion() {
        final double minimalExpectedSpeedup = 1.5;
        final TestOutcome outcome = runTestWithTiming(2_000_000, false, 2);
        recordSpeedupSummary("testParSimpleTwoMillion", minimalExpectedSpeedup,
            outcome.actualSpeedup, outcome.elapsedMillis, 2);
    }

    /**
     * Prueba que la implementación de dos tareas en paralelo calcula correctamente los resultados para arreglos con cientos de millones de elementos..
     */
    public void testParSimpleTwoHundredMillion() {
        final double minimalExpectedSpeedup = 1.5;
        final TestOutcome outcome = runTestWithTiming(200_000_000, false, 2);
        recordSpeedupSummary("testParSimpleTwoHundredMillion", minimalExpectedSpeedup,
            outcome.actualSpeedup, outcome.elapsedMillis, 2);
    }

    /**
     * Prueba que la implementación de muchas tareas en paralelo calcula correctamente los resultados para arreglos con un millónde elementos.
     */
    public void testParManyTaskTwoMillion() {
        final int ncores = getNCores();
        final double minimalExpectedSpeedup = (double)ncores * 0.6;
        final TestOutcome outcome = runTestWithTiming(2_000_000, true, ncores);
        recordSpeedupSummary("testParManyTaskTwoMillion", minimalExpectedSpeedup,
            outcome.actualSpeedup, outcome.elapsedMillis, ncores);
    }

    /**
     * Prueba que la implementación de muchas tareas en paralelo calcula correctamente los resultados para arreglos con cientos de millones de elementos.
     */
    public void testParManyTaskTwoHundredMillion() {
        final int ncores = getNCores();
        final double minimalExpectedSpeedup = (double)ncores * 0.8;
        final TestOutcome outcome = runTestWithTiming(200_000_000, true, ncores);
        recordSpeedupSummary("testParManyTaskTwoHundredMillion", minimalExpectedSpeedup,
            outcome.actualSpeedup, outcome.elapsedMillis, ncores);
    }
}
