package com.tagtraum.jipes.math;

public abstract class FFTFactory {

    protected FFTFactory() {
    }

    public abstract Transform create(int numberOfSamples);

    public static class JavaFFT implements Transform {

        private static final int MAX_FAST_BITS = 16;
        private static final int[][] FFT_BIT_TABLE = new int[MAX_FAST_BITS][];
        private final int numberOfSamples;
        private final int[] reverseIndices;
        private float[] frequencies;
        private final float[] cosTable;
        private final float[] sinTable;

        static {
            int len = 2;
            for (int b = 1; b <= MAX_FAST_BITS; b++) {
                FFT_BIT_TABLE[b - 1] = new int[len];
                for (int i = 0; i < len; i++) {
                    FFT_BIT_TABLE[b - 1][i] = reverseBits(i, b);
                }
                len <<= 1;
            }
        }

        public JavaFFT(final int numberOfSamples) {
            if (!isPowerOfTwo(numberOfSamples)) throw new IllegalArgumentException("N is not a power of 2");
            if (numberOfSamples <=0) throw new IllegalArgumentException("N must be greater than 0");
            this.numberOfSamples = numberOfSamples;
            final int numberOfBits = getNumberOfNeededBits(numberOfSamples);
            this.reverseIndices = new int[numberOfSamples];
            for (int i = 0; i < numberOfSamples; i++) {
                final int j = fastReverseBits(i, numberOfBits);
                this.reverseIndices[i] = j;
            }
            this.frequencies = new float[numberOfSamples];
            for (int index=0; index<numberOfSamples; index++) {
                if (index <= numberOfSamples / 2) {
                    this.frequencies[index] = index / (float)numberOfSamples;
                } else {
                    this.frequencies[index] = -((numberOfSamples - index) / (float) numberOfSamples);
                }
            }

            this.cosTable = new float[numberOfSamples];
            this.sinTable = new float[numberOfSamples];
            for (int i = 0; i < numberOfSamples; i++) {
                float angle = (float)(-2.0 * Math.PI * i / numberOfSamples);
                cosTable[i] = (float)Math.cos(angle);
                sinTable[i] = (float)Math.sin(angle);
            }
        }

        public float[][] inverseTransform(final float[] real, final float[] imaginary) throws UnsupportedOperationException {
            final float[][] out = new float[2][real.length];
            transform(true, real, imaginary, out[0], out[1]);
            return out;
        }

        public float[][] transform(final float[] real) throws UnsupportedOperationException {
            final float[][] out = new float[3][real.length];
            transform(false, real, null, out[0], out[1]);
            out[2] = frequencies.clone();
            return out;
        }

        public float[][] transform(final float[] real, final float[] imaginary) throws UnsupportedOperationException {
            final float[][] out = new float[3][real.length];
            transform(false, real, imaginary, out[0], out[1]);
            out[2] = frequencies.clone();
            return out;
        }

        public void transform(final boolean inverse,
                              final float[] realIn,
                              final float[] imaginaryIn,
                              final float[] realOut,
                              final float[] imaginaryOut) {
            if (realIn.length != numberOfSamples) {
                throw new IllegalArgumentException("Number of samples must be " + numberOfSamples + " for this instance of JavaFFT");
            }

            int i = 0;
            for (; i <= numberOfSamples - 4; i += 4) {
                realOut[reverseIndices[i]] = realIn[i];
                realOut[reverseIndices[i+1]] = realIn[i+1];
                realOut[reverseIndices[i+2]] = realIn[i+2];
                realOut[reverseIndices[i+3]] = realIn[i+3];
            }
            for (; i < numberOfSamples; i++) {
                realOut[reverseIndices[i]] = realIn[i];
            }

            if (imaginaryIn != null) {
                i = 0;
                for (; i <= numberOfSamples - 4; i += 4) {
                    imaginaryOut[reverseIndices[i]] = imaginaryIn[i];
                    imaginaryOut[reverseIndices[i+1]] = imaginaryIn[i+1];
                    imaginaryOut[reverseIndices[i+2]] = imaginaryIn[i+2];
                    imaginaryOut[reverseIndices[i+3]] = imaginaryIn[i+3];
                }
                for (; i < numberOfSamples; i++) {
                    imaginaryOut[reverseIndices[i]] = imaginaryIn[i];
                }
            } else {
                for (i = 0; i < numberOfSamples; i++) {
                    imaginaryOut[i] = 0.0f;
                }
            }

            int blockEnd = 1;
            final int sign = inverse ? 1 : -1;

            for (int blockSize = 2; blockSize <= numberOfSamples; blockSize <<= 1) {
                final int angleStep = numberOfSamples / blockSize;

                for (i = 0; i < numberOfSamples; i += blockSize) {
                    for (int j = i, n = 0; n < blockEnd; j++, n++) {
                        final int angleIdx = (n * angleStep) & (numberOfSamples - 1);
                        final float ar0 = cosTable[angleIdx];
                        final float ai0 = sign * sinTable[angleIdx];

                        final int k = j + blockEnd;
                        final float rk = realOut[k];
                        final float ik = imaginaryOut[k];

                        final float tr = ar0 * rk - ai0 * ik;
                        final float ti = ar0 * ik + ai0 * rk;

                        realOut[k] = realOut[j] - tr;
                        imaginaryOut[k] = imaginaryOut[j] - ti;

                        realOut[j] += tr;
                        imaginaryOut[j] += ti;
                    }
                }

                blockEnd = blockSize;
            }

            if (inverse) {
                final float scale = 1.0f / numberOfSamples;
                i = 0;
                for (; i <= numberOfSamples - 4; i += 4) {
                    realOut[i] *= scale;
                    imaginaryOut[i] *= scale;
                    realOut[i+1] *= scale;
                    imaginaryOut[i+1] *= scale;
                    realOut[i+2] *= scale;
                    imaginaryOut[i+2] *= scale;
                    realOut[i+3] *= scale;
                    imaginaryOut[i+3] *= scale;
                }
                for (; i < numberOfSamples; i++) {
                    realOut[i] *= scale;
                    imaginaryOut[i] *= scale;
                }
            }
        }

        private static int getNumberOfNeededBits(final int powerOfTwo) {
            for (int i = 0; true; i++) {
                final int j = powerOfTwo & 1 << i;
                if (j != 0) return i;
            }
        }

        private static int reverseBits(final int index, final int numberOfBits) {
            int ind = index;
            int rev = 0;
            for (int i = 0; i < numberOfBits; i++) {
                rev = rev << 1 | ind & 1;
                ind >>= 1;
            }
            return rev;
        }

        private static int fastReverseBits(final int index, final int numberOfBits) {
            if (numberOfBits <= MAX_FAST_BITS)
                return FFT_BIT_TABLE[numberOfBits - 1][index];
            else
                return reverseBits(index, numberOfBits);
        }

        private static boolean isPowerOfTwo(final int number) {
            return (number & (number - 1)) == 0;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final JavaFFT javaFFT = (JavaFFT) o;
            if (numberOfSamples != javaFFT.numberOfSamples) return false;
            return true;
        }

        @Override
        public int hashCode() {
            return numberOfSamples;
        }

        @Override
        public String toString() {
            return "JavaFFT{" +
                    "N=" + numberOfSamples +
                    '}';
        }
    }
}