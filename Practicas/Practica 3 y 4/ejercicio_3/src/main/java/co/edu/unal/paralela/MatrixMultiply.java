package co.edu.unal.paralela;

import static edu.rice.pcdp.PCDP.forall2d;
import static edu.rice.pcdp.PCDP.forall2dChunked;
import static edu.rice.pcdp.PCDP.forseq2d;

/**
 * Clase envolvente pata implementar de forma eficiente la multiplicación dde matrices en paralelo.
 */
public final class MatrixMultiply {
    /**
     * Constructor por omisión.
     */
    private MatrixMultiply() {
    }

    /**
     * Realiza una multiplicación de matrices bidimensionales (A x B = C) de forma secuencial.
     *
     * @param A Una matriz de entrada con dimensiones NxN
     * @param B Una matriz de entrada con dimensiones NxN
     * @param C Matriz de salida
     * @param N Tamaño de las matrices de entrada
     */
    public static void seqMatrixMultiply(final double[][] A, final double[][] B,
            final double[][] C, final int N) {
        forseq2d(0, N - 1, 0, N - 1, (i, j) -> {
            C[i][j] = 0.0;
            for (int k = 0; k < N; k++) {
                C[i][j] += A[i][k] * B[k][j];
            }
        });
    }

    /**
     * Realiza una multiplicación de matrices bidimensionales (A x B = C) de forma paralela con una tarea para cada operacion.
     *
     * @param A Una matriz de entrada con dimensiones NxN
     * @param B Una matriz de entrada con dimensiones NxN
     * @param C Matriz de salida
     * @param N amaño de las matrices de entrada
     */
    public static void parMatrixMultiply(final double[][] A, final double[][] B,
            final double[][] C, final int N) {
        forall2dChunked(0, N - 1, 0, N - 1, (i, j) -> {
            double sum = 0.0;
            for (int k = 0; k < N; k++) {
                sum += A[i][k] * B[k][j];
            }
            C[i][j] = sum;
        });
    }

    /**
     * Realiza una multiplicación de matrices bidimensionales (A x B = C) de forma paralela con chunks.
     *
     * @param A Una matriz de entrada con dimensiones NxN
     * @param B Una matriz de entrada con dimensiones NxN
     * @param C Matriz de salida
     * @param N amaño de las matrices de entrada
     */
    public static void parMatrixMultiplyChunked(final double[][] A, final double[][] B,
            final double[][] C, final int N) {
        forall2dChunked(0, N - 1, 0, N - 1, (i, j) -> {
            double sum = 0.0;
            for (int k = 0; k < N; k++) {
                sum += A[i][k] * B[k][j];
            }
            C[i][j] = sum;
        });
    }
}
