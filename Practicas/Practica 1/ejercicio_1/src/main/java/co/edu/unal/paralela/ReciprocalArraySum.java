package co.edu.unal.paralela;

import java.util.concurrent.RecursiveAction;

/**
 * Clase que contiene los métodos para implementar la suma de los recíprocos de un arreglo usando paralelismo.
 */
public final class ReciprocalArraySum {

    /**
     * Constructor.
     */
    private ReciprocalArraySum() {
    }

    /**
     * Calcula secuencialmente la suma de valores recíprocos para un arreglo.
     *
     * @param input Arreglo de entrada
     * @return La suma de los recíprocos del arreglo de entrada
     */
    protected static double seqArraySum(final double[] input) {
        double sum = 0;

        // Calcula la suma de los recíprocos de los elementos del arreglo
        for (int i = 0; i < input.length; i++) {
            sum += 1 / input[i];
        }

        return sum;
    }

    /**
     * calcula el tamaño de cada trozo o sección, de acuerdo con el número de secciones para crear
     * a través de un número dado de elementos.
     *
     * @param nChunks El número de secciones (chunks) para crear
     * @param nElements El número de elementos para dividir
     * @return El tamaño por defecto de la sección (chunk)
     */
    private static int getChunkSize(final int nChunks, final int nElements) {
        // Función techo entera
        return (nElements + nChunks - 1) / nChunks;
    }

    /**
     * Calcula el índice del elemento inclusivo donde la sección/trozo (chunk) inicia,
     * dado que hay cierto número de secciones/trozos (chunks).
     *
     * @param chunk la sección/trozo (chunk) para cacular la posición de inicio
     * @param nChunks Cantidad de secciones/trozos (chunks) creados
     * @param nElements La cantidad de elementos de la sección/trozo que deben atravesarse
     * @return El índice inclusivo donde esta sección/trozo (chunk) inicia en el conjunto de 
     *         nElements
     */
    private static int getChunkStartInclusive(final int chunk,
            final int nChunks, final int nElements) {
        final int chunkSize = getChunkSize(nChunks, nElements);
        return chunk * chunkSize;
    }

    /**
     * Calcula el índice del elemento exclusivo que es proporcionado al final de la sección/trozo (chunk),
     * dado que hay cierto número de secciones/trozos (chunks).
     *
     * @param chunk La sección para calcular donde termina
     * @param nChunks Cantidad de secciones/trozos (chunks) creados
     * @param nElements La cantidad de elementos de la sección/trozo que deben atravesarse
     * @return El índice de terminación exclusivo para esta sección/trozo (chunk)
     */
    private static int getChunkEndExclusive(final int chunk, final int nChunks,
            final int nElements) {
        final int chunkSize = getChunkSize(nChunks, nElements);
        final int end = (chunk + 1) * chunkSize;
        if (end > nElements) {
            return nElements;
        } else {
            return end;
        }
    }

    /**
     * Bloque que define la tarea que suma los recíprocos de una porción del arreglo.
     * Cada instancia representa una tarea que puede ejecutarse en el framework Fork/Join.
     */
    private static class ReciprocalArraySumTask extends RecursiveAction {
        /**
         * Iniciar el índice para el recorrido transversal hecho por esta tarea.
         */
        private final int startIndexInclusive;
        /**
         * Concluir el índice para el recorrido transversal hecho por esta tarea.
         */
        private final int endIndexExclusive;
        /**
         * Arreglo de entrada para la suma de recíprocos.
         */
        private final double[] input;
        /**
         * Valor intermedio producido por esta tarea.
         */
        private double value;

        /**
         * Constructor.
         * @param setStartIndexInclusive establece el índice inicial para comenzar
         *        el recorrido trasversal.
         * @param setEndIndexExclusive establece el índice final para el recorrido trasversal.
         * @param setInput Valores de entrada
         */
        ReciprocalArraySumTask(final int setStartIndexInclusive,
                final int setEndIndexExclusive, final double[] setInput) {
            this.startIndexInclusive = setStartIndexInclusive;
            this.endIndexExclusive = setEndIndexExclusive;
            this.input = setInput;
        }

        /**
         * Adquiere el valor calculado por esta tarea.
         * @return El valor calculado por esta tarea
         */
        public double getValue() {
            return value;
        }

        @Override
        protected void compute() {
            double local = 0.0;
            for (int i = startIndexInclusive; i < endIndexExclusive; i++) {
                local += 1 / input[i];
            }
            value = local;
        }
    }

    /**
     * Para hacer: Modificar este método para calcular la misma suma de recíprocos como le realizada en
        * seqArraySum, pero utilizando dos tareas ejecutándose en paralelo dentro del framework ForkJoin de Java.
        * Se puede asumir que la longitud del arreglo de entrada es divisible por 2.
     *
     * @param input Arreglo de entrada
     * @return La suma de los recíprocos del arreglo de entrada
     */
    protected static double parArraySum(final double[] input) {
        assert input.length % 2 == 0;

        final int mid = input.length / 2;
        final ReciprocalArraySumTask left = new ReciprocalArraySumTask(0, mid, input);
        final ReciprocalArraySumTask right = new ReciprocalArraySumTask(mid, input.length, input);

        left.fork();
        right.compute();
        left.join();

        return left.getValue() + right.getValue();
    }

    /**
     * Para hacer: extender el trabajo hecho en parArraySum para permitir usar un número
     * arbitrario de tareas y calcular la suma del arreglo recíproco en paralelo.
     * Los helpers `getChunkStartInclusive` y `getChunkEndExclusive` se proporcionan
     * para calcular los índices de inicio/fin de cada chunk de forma consistente.
     *
     * @param input Arreglo de entrada
     * @param numTasks El número de tareas para crear
     * @return La suma de los recíprocos del arreglo de entrada
     */
    protected static double parManyTaskArraySum(final double[] input,
            final int numTasks) {
        final int taskCount = Math.min(numTasks, input.length);
        if (taskCount <= 1) {
            return seqArraySum(input);
        }

        final ReciprocalArraySumTask[] tasks = new ReciprocalArraySumTask[taskCount];

        for (int i = 0; i < taskCount; i++) {
            final int startIndexInclusive = getChunkStartInclusive(i, taskCount, input.length);
            final int endIndexExclusive = getChunkEndExclusive(i, taskCount, input.length);
            tasks[i] = new ReciprocalArraySumTask(startIndexInclusive,
                endIndexExclusive, input);
        }

        for (int i = 0; i < taskCount - 1; i++) {
            tasks[i].fork();
        }

        tasks[taskCount - 1].compute();

        double sum = tasks[taskCount - 1].getValue();
        for (int i = 0; i < taskCount - 1; i++) {
            final ReciprocalArraySumTask task = tasks[i];
            task.join();
            sum += task.getValue();
        }

        return sum;
    }
}
