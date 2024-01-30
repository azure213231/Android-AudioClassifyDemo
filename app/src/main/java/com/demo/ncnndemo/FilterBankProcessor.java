package com.demo.ncnndemo;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.MathArrays;

import java.util.ArrayList;
import java.util.List;

public class FilterBankProcessor {

    public static double[][][] getFeature(double[] waveforms){
        double[][][] feature = FilterBankProcessor.extractFbank(waveforms);

        double[][][] transposedFeature = transposeFeature(feature);
        double[][][] normalizeFeature = normalizeFeature(transposedFeature);
        double[] inputLensRatio = {1.0};
        double[][][] extendMaskRatios = extendMaskRatios(normalizeFeature, inputLensRatio);
        return extendMaskRatios;
    }

    private static double[][][] extractFbank(double[] waveforms){
        List<double[][]> logFbanks = new ArrayList<>();

//        for (double[] waveform : waveforms) {
//            if (waveform.length == 1) {
//                // If waveform is 1D, convert it to a 2D array (assuming it represents a single channel)
//                waveform = new double[][]{waveform};
//            }
            double[] waveform = waveforms;
            double[][] logFbank = computeFilterBanks(waveform);
            // Transpose logFbank
            double[][] logFbankNew = transpose(logFbank);
            logFbanks.add(logFbankNew);
//        }

        // Convert List<double[][]> to a 3D array
        double[][][] logFbanksArray = logFbanks.toArray(new double[0][][]);

        return logFbanksArray;
    }
    private static double[][] computeFilterBanks(double[] waveform) {
        int sampleRate = 16000;
        double epsilon = 1.1921e-07;

        //预加重、加窗分帧
        double[][] stridedInput = generatePoveyWindow(waveform,0.0f,true,true,0.97f);

        // Compute FFT
        // 傅里叶变换
        double[][] spectrum = computeFFT(stridedInput,true);

        // Compute Mel filter banks
        // 计算梅尔滤波器组
        double[][] melEnergies = getMelBanks(80,512, sampleRate, 20.0,0.0,100,-500,1.0);

//      pad right column with zeros and add dimension, size (num_mel_bins, padded_window_size // 2 + 1)
//      对梅尔滤波器组进行分窗
        double[][] paddedMelEnergies = padMelEnergies(melEnergies);

        //对数据进行矩阵迅运算提取特征
        double[][] calculateMelEnergies = calculateMelEnergies(spectrum, paddedMelEnergies);

        //将结果取对数
        double[][] applyLogFbank = applyLogFbank(calculateMelEnergies);

        // 进行减去列均值
        boolean subtractMean = false;
        double[][] logFbankArray = subtractColumnMean(applyLogFbank, subtractMean);

        return logFbankArray;
    }

    private static double[][][] normalizeFeature(double[][][] feature) {
        int numChannels = feature.length;
        int numRows = feature[0].length;
        int numCols = feature[0][0].length;

        double[][][] normalizedFeature = new double[numChannels][numRows][numCols];

        // Calculate channel-wise and column-wise means
        double[][] columnMeans = new double[numChannels][numCols];
        for (int k = 0; k < numChannels; k++) {
            for (int j = 0; j < numCols; j++) {
                for (int i = 0; i < numRows; i++) {
                    columnMeans[k][j] += feature[k][i][j];
                }
                columnMeans[k][j] /= numRows;
            }
        }

        // Subtract column-wise means
        for (int k = 0; k < numChannels; k++) {
            for (int i = 0; i < numRows; i++) {
                for (int j = 0; j < numCols; j++) {
                    normalizedFeature[k][i][j] = feature[k][i][j] - columnMeans[k][j];
                }
            }
        }

        return normalizedFeature;
    }

    private static double[][] transpose(double[][] logFbankArray) {
        int numRows = logFbankArray.length;
        int numCols = logFbankArray[0].length;

        // Create a new array for the transposed data
        double[][] transposedArray = new double[numCols][numRows];

        // Perform the transposition
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numCols; j++) {
                transposedArray[j][i] = logFbankArray[i][j];
            }
        }

        return transposedArray;
    }

    // Assuming feature is a 3D array
    private static double[][][] extendMaskRatios(double[][][] feature, double[] inputLensRatio) {
        int numChannels = feature.length;
        int numRows = feature[0].length;
        int numCols = feature[0][0].length;

        // Calculate input_lens
        double[] inputLens = new double[numChannels];
        for (int k = 0; k < numChannels; k++) {
            inputLens[k] = inputLensRatio[0] * numRows;
        }

        // Calculate mask_lens
        int[] maskLens = new int[numChannels];
        for (int k = 0; k < numChannels; k++) {
            maskLens[k] = Math.round((float) inputLens[k]);
        }

        // Generate mask tensor
        int[][][] idxs = new int[numChannels][numRows][numCols];
        for (int k = 0; k < numChannels; k++) {
            for (int i = 0; i < numRows; i++) {
                for (int j = 0; j < numCols; j++) {
                    idxs[k][i][j] = j;
                }
            }
        }

        boolean[][][] mask = new boolean[numChannels][numRows][numCols];
        for (int k = 0; k < numChannels; k++) {
            for (int i = 0; i < numRows; i++) {
                for (int j = 0; j < numCols; j++) {
                    mask[k][i][j] = idxs[k][i][j] < maskLens[k];
                }
            }
        }

        // Apply mask to feature
        double[][][] featureMasked = new double[numChannels][numRows][numCols];
        for (int k = 0; k < numChannels; k++) {
            for (int i = 0; i < numRows; i++) {
                for (int j = 0; j < numCols; j++) {
                    featureMasked[k][i][j] = mask[k][i][j] ? feature[k][i][j] : 0.0;
                }
            }
        }

        return featureMasked;
    }

    private static double[][][] transposeFeature(double[][][] feature) {
        int dim1 = feature.length;
        int dim2 = feature[0].length;
        int dim3 = feature[0][0].length;

        double[][][] transposedFeature = new double[dim1][dim3][dim2];

        for (int i = 0; i < dim1; i++) {
            for (int j = 0; j < dim2; j++) {
                for (int k = 0; k < dim3; k++) {
                    transposedFeature[i][k][j] = feature[i][j][k];
                }
            }
        }

        return transposedFeature;
    }

    private static double[][] subtractColumnMean(double[][] matrix, boolean subtractMean) {
        int numRows = matrix.length;
        int numCols = matrix[0].length;

        if (subtractMean) {
            // Calculate column means
            double[] colMeans = new double[numCols];
            for (int j = 0; j < numCols; j++) {
                double sum = 0.0;
                for (int i = 0; i < numRows; i++) {
                    sum += matrix[i][j];
                }
                colMeans[j] = sum / numRows;
            }

            // Subtract column means from the tensor
            for (int i = 0; i < numRows; i++) {
                for (int j = 0; j < numCols; j++) {
                    matrix[i][j] -= colMeans[j];
                }
            }
        }

        return matrix;
    }

    private static double[][] applyLogFbank(double[][] melEnergies) {
        int numRows = melEnergies.length;
        int numCols = melEnergies[0].length;

        // Apply log to avoid log of zero
        double epsilon = getEpsilon();  // Replace with the actual method to get epsilon
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numCols; j++) {
                melEnergies[i][j] = Math.log(Math.max(melEnergies[i][j], epsilon));
            }
        }

        return melEnergies;
    }

    private static double getEpsilon() {
        // Choose an appropriate small value for epsilon
        return 1e-10;  // Example epsilon value, replace with your logic
    }

    private static double[][] calculateMelEnergies(double[][] spectrum, double[][] melFilters) {
        int numRows = spectrum.length;
        int numCols = melFilters[0].length;

        RealMatrix spectrumMatrix = new Array2DRowRealMatrix(spectrum);
        RealMatrix melFiltersMatrix = new Array2DRowRealMatrix(melFilters);
        RealMatrix melEnergiesMatrix = spectrumMatrix.multiply(melFiltersMatrix.transpose());

        return melEnergiesMatrix.getData();
    }

    private static double[][] getStrided(double[] waveform, int windowSize, int windowShift, boolean snipEdges) {
        int numSamples = waveform.length;

        if (snipEdges) {
            if (numSamples < windowSize) {
                return new double[0][0];
            } else {
                int m = 1 + (numSamples - windowSize) / windowShift;
                double[][] stridedInput = new double[m][windowSize];

                for (int i = 0; i < m; i++) {
                    stridedInput[i] = new double[windowSize];
                    for (int j = 0; j < windowSize; j++) {
                        stridedInput[i][j] = waveform[i * windowShift + j];
                    }
                }

                return stridedInput;
            }
        } else {
            double[] reversedWaveform = new double[numSamples];
            for (int i = 0; i < numSamples; i++) {
                reversedWaveform[i] = waveform[numSamples - 1 - i];
            }

            int m = (numSamples + (windowShift / 2)) / windowShift;
            int pad = windowSize / 2 - windowShift / 2;

            if (pad > 0) {
                double[] padLeft = new double[pad];
                System.arraycopy(reversedWaveform, reversedWaveform.length - pad, padLeft, 0, pad);
                double[] padRight = reversedWaveform;
                waveform = new double[padLeft.length + waveform.length + padRight.length];
                System.arraycopy(padLeft, 0, waveform, 0, padLeft.length);
                System.arraycopy(reversedWaveform, 0, waveform, padLeft.length, reversedWaveform.length);
                System.arraycopy(padRight, 0, waveform, padLeft.length + reversedWaveform.length, padRight.length);
            } else {
                waveform = new double[reversedWaveform.length - pad];
                System.arraycopy(reversedWaveform, pad, waveform, 0, waveform.length);
            }

            double[][] stridedInput = new double[m][windowSize];
            for (int i = 0; i < m; i++) {
                int startIdx = i * windowShift;
                for (int j = 0; j < windowSize; j++) {
                    stridedInput[i][j] = waveform[startIdx + j];
                }
            }

            return stridedInput;
        }
    }

    private static double[][] generatePoveyWindow(double[] waveform,float dither,boolean removeDCOffset,boolean rawEnergy,float preemphasisCoefficient) {
        int windowSize = 400;
        int windowShift = 160;
        int paddedWindowSize = 512;
        double epsilon = 1.1921e-07;
        float energyFloor = 1.0f;

        //根据窗口大小偏移量切分数据
        double[][] stridedInput = getStrided(waveform, windowSize, windowShift, true);

        // If dither is not 0.0
        if (dither != 0.0) {
            double[][] randGauss = new double[stridedInput.length][stridedInput[0].length];
            for (int i = 0; i < stridedInput.length; i++) {
                for (int j = 0; j < stridedInput[0].length; j++) {
                    randGauss[i][j] = Math.random();  // Simulating torch.randn
                }
            }
            // strided_input = strided_input + rand_gauss * dither
            for (int i = 0; i < stridedInput.length; i++) {
                for (int j = 0; j < stridedInput[0].length; j++) {
                    stridedInput[i][j] += randGauss[i][j] * dither;
                }
            }
        }

        // If removeDCOffset is true
        if (removeDCOffset) {
            // Subtract each row/frame by its mean
            int numRows = stridedInput.length;
            int numCols = stridedInput[0].length;

            // Calculate row means
            double[] rowMeans = new double[numRows];
            for (int i = 0; i < numRows; i++) {
                double rowSum = 0.0;
                for (int j = 0; j < numCols; j++) {
                    rowSum += stridedInput[i][j];
                }
                rowMeans[i] = rowSum / numCols;
            }

            // Subtract each row/frame by its mean
            for (int i = 0; i < numRows; i++) {
                for (int j = 0; j < numCols; j++) {
                    stridedInput[i][j] -= rowMeans[i];
                }
            }

        }

        double[] signalLogEnergy = null;

        // If rawEnergy is true
        if (rawEnergy) {
            // Compute the log energy of each row/frame before applying preemphasis and window function
            signalLogEnergy = getLogEnergy(stridedInput,epsilon , energyFloor);
        }

        // If preemphasisCoefficient is not 0.0 修正系数
        if (preemphasisCoefficient != 0.0) {
            // strided_input[i,j] -= preemphasis_coefficient * strided_input[i, max(0, j-1)] for all i,j
            double[][] offsetStridedInput = padArray(stridedInput, 1, 0);  // Simulating torch.nn.functional.pad
            for (int i = 0; i < stridedInput.length; i++) {
                for (int j = 0; j < stridedInput[0].length; j++) {
                    stridedInput[i][j] -= preemphasisCoefficient * offsetStridedInput[i][j];
                }
            }
        }

        double power = 0.85f;
        // Apply window_function to each row/frame
        double[][] windowFunction = featureWindowFunction(windowSize,false, power);
        for (int i = 0; i < stridedInput.length; i++) {
            for (int j = 0; j < stridedInput[0].length; j++) {
                stridedInput[i][j] *= windowFunction[0][j];
            }
        }

        // Pad columns with zero until we reach size (m, padded_window_size)
        if (paddedWindowSize != windowSize) {
            int paddingRight = paddedWindowSize - windowSize;
            stridedInput = padArray(stridedInput, 0, paddingRight);
        }

        // Compute energy after window function (not the raw one)
        if (!rawEnergy) {
            signalLogEnergy = getLogEnergy(stridedInput, epsilon, energyFloor);
        }

        return stridedInput;
    }

    private static double[][] featureWindowFunction(int windowSize, boolean periodic, double power) {
        double[][] window = new double[1][windowSize];  // Assuming 2D array for consistency

        for (int i = 0; i < windowSize; i++) {
            double angle = 2.0 * Math.PI * i / (windowSize - (periodic ? 0 : 1));
            window[0][i] = 0.5 * (1.0 - Math.cos(angle));
        }

        if (power != 1.0) {
            for (int i = 0; i < windowSize; i++) {
                window[0][i] = FastMath.pow(window[0][i], power);
            }
        }

        return window;
    }

    private static double[][] padArray(double[][] inputArray, int padLeft, int padRight) {
        int numRows = inputArray.length;
        int numCols = inputArray[0].length;

        // Pad the left side
        double[][] paddedArray = new double[numRows][numCols + padLeft + padRight];
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < padLeft; j++) {
                paddedArray[i][j] = inputArray[i][0];  // Use the value of the first element for padding
            }
            // Copy the original data
            System.arraycopy(inputArray[i], 0, paddedArray[i], padLeft, numCols);
        }

        // Pad the right side
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < padRight; j++) {
                paddedArray[i][numCols + padLeft + j] = 0.0;
            }
        }

        return paddedArray;
    }

    private static double[] getLogEnergy(double[][] stridedInput, double epsilon, float energyFloor) {
        int numRows = stridedInput.length;
        double[] logEnergy = new double[numRows];

        for (int i = 0; i < numRows; i++) {
            double rowSum = 0.0;
            for (int j = 0; j < stridedInput[0].length; j++) {
                rowSum += Math.pow(stridedInput[i][j], 2);
            }
            logEnergy[i] = Math.max(rowSum, epsilon);
        }

        for (int i = 0; i < numRows; i++) {
            logEnergy[i] = Math.log(logEnergy[i]);
            if (energyFloor != 0.0) {
                logEnergy[i] = Math.max(logEnergy[i], Math.log(energyFloor));
            }
        }

        return logEnergy;
    }

    private static double[][] computeFFT(double[][] stridedInput,boolean usePower) {
        int numRows = stridedInput.length;
        int numCols = stridedInput[0].length;

        // Perform FFT
        FastFourierTransformer transformer = new FastFourierTransformer(DftNormalization.STANDARD);
        double[][] spectrum = new double[numRows][numCols / 2 + 1];

        for (int i = 0; i < numRows; i++) {
            // Convert real input to complex input
            Complex[] complexInput = new Complex[numCols];
            for (int j = 0; j < numCols; j++) {
                complexInput[j] = new Complex(stridedInput[i][j], 0.0);
            }

            // Perform FFT
            Complex[] complexSpectrum = transformer.transform(complexInput,TransformType.FORWARD);

            // Compute absolute values
            for (int j = 0; j < spectrum[i].length; j++) {
                spectrum[i][j] = complexSpectrum[j].abs();
            }

            if (usePower) {
                for (int j = 0; j < spectrum[i].length; j++) {
                    spectrum[i][j] = Math.pow(spectrum[i][j], 2.0);
                }
            }
        }

        return spectrum;
    }

    private static double[][] getMelBanks(int numBins, int windowLengthPadded, double sampleFreq,
                                         double lowFreq, double highFreq, double vtlnLow, double vtlnHigh,
                                         double vtlnWarpFactor) {
        assert numBins > 3 : "Must have at least 3 mel bins";
        assert windowLengthPadded % 2 == 0;

        int numFftBins = windowLengthPadded / 2;
        double nyquist = 0.5 * sampleFreq;

        if (highFreq <= 0.0) {
            highFreq += nyquist;
        }

        assert (0.0 <= lowFreq && lowFreq < nyquist) && (0.0 < highFreq && highFreq <= nyquist && lowFreq < highFreq) :
                "Bad values in options: low-freq " + lowFreq + " and high-freq " + highFreq + " vs. nyquist " + nyquist;

        double fftBinWidth = sampleFreq / windowLengthPadded;
        double melLowFreq = melScaleScalar(lowFreq);
        double melHighFreq = melScaleScalar(highFreq);
        double melFreqDelta = (melHighFreq - melLowFreq) / (numBins + 1);

        if (vtlnHigh < 0.0) {
            vtlnHigh += nyquist;
        }

        assert vtlnWarpFactor == 1.0 || ((lowFreq < vtlnLow && vtlnLow < highFreq) && (0.0 < vtlnHigh && vtlnHigh < highFreq)
                && (vtlnLow < vtlnHigh)) : "Bad values in options: vtln-low " + vtlnLow + " and vtln-high " + vtlnHigh +
                ", versus low-freq " + lowFreq + " and high-freq " + highFreq;

        double[][] bins = new double[numBins][numFftBins];
        double[][] leftMel = new double[numBins][1];
        double[][] centerMel = new double[numBins][1];
        double[][] rightMel = new double[numBins][1];

        for (int i = 0; i < numBins; i++) {
            leftMel[i][0] = melLowFreq + i * melFreqDelta;
            centerMel[i][0] = melLowFreq + (i + 1.0) * melFreqDelta;
            rightMel[i][0] = melLowFreq + (i + 2.0) * melFreqDelta;

            if (vtlnWarpFactor != 1.0) {
                leftMel[i] = vtlnWarpMelFreq(vtlnLow, vtlnHigh, lowFreq, highFreq, vtlnWarpFactor, leftMel[i]);
                centerMel[i] = vtlnWarpMelFreq(vtlnLow, vtlnHigh, lowFreq, highFreq, vtlnWarpFactor, centerMel[i]);
                rightMel[i] = vtlnWarpMelFreq(vtlnLow, vtlnHigh, lowFreq, highFreq, vtlnWarpFactor, rightMel[i]);
            }
        }

//        double[] centerFreqs = inverseMelScale(centerMel);

        double[][] mel = melScale(fftBinWidth, numFftBins);

        double[][] upSlope = new double[numBins][numFftBins];
        double[][] downSlope = new double[numBins][numFftBins];

        for (int i = 0; i < numBins; i++) {
            for (int j = 0; j < numFftBins; j++) {
                upSlope[i][j] = (mel[0][j] - leftMel[i][0]) / (centerMel[i][0] - leftMel[i][0]);
                downSlope[i][j] = (rightMel[i][0] - mel[0][j]) / (rightMel[i][0] - centerMel[i][0]);
            }
        }

        // Compute bins based on vtlnWarpFactor
        for (int i = 0; i < numBins; i++) {
            for (int j = 0; j < numFftBins; j++) {
                if (vtlnWarpFactor == 1.0) {
                    // leftMel < centerMel < rightMel, so we can min the two slopes and clamp negative values
                    bins[i][j] = Math.max(0.0, Math.min(upSlope[i][j], downSlope[i][j]));
                } else {
                    // Warping can move the order of leftMel, centerMel, rightMel anywhere
                    if (mel[i][j] > leftMel[i][0] && mel[i][j] <= centerMel[i][0]) {
                        bins[i][j] = upSlope[i][j];
                    } else if (mel[i][j] > centerMel[i][0] && mel[i][j] < rightMel[i][0]) {
                        bins[i][j] = downSlope[i][j];
                    }
                }
            }
        }

        return bins;
    }

    private static double[] vtlnWarpMelFreq(double vtlnLowCutoff, double vtlnHighCutoff, double lowFreq, double highFreq,
                                           double vtlnWarpFactor, double[] melFreq) {
        return melScale(vtlnWarpFreq(vtlnLowCutoff, vtlnHighCutoff, lowFreq, highFreq, vtlnWarpFactor, inverseMelScale(melFreq)));
    }

    private static double[] inverseMelScale(double[] melFrequencies) {
        double[] result = new double[melFrequencies.length];

        for (int i = 0; i < melFrequencies.length; i++) {
            result[i] = 700.0 * (FastMath.exp(melFrequencies[i] / 1127.0) - 1.0);
        }

        return result;
    }

    private static double[] melScale(double[] frequencies) {
        double[] result = new double[frequencies.length];

        for (int i = 0; i < frequencies.length; i++) {
            result[i] = 1127.0 * FastMath.log(1.0 + frequencies[i] / 700.0);
        }

        return result;
    }

    private static double[][] melScale(double fftBinWidth, int numFftBins) {
        double[] mel = new double[numFftBins];
        for (int i = 0; i < numFftBins; i++) {
            mel[i] = 1127.0 * Math.log(1.0 + (fftBinWidth * i) / 700.0);
        }

        // Assuming you have a method to unsqueeze the array (add an extra dimension)
        return unsqueeze(mel);
    }

    // Example unsqueeze method (modify based on your needs)
    private static double[][] unsqueeze(double[] array) {
        double[][] result = new double[1][array.length];
        System.arraycopy(array, 0, result[0], 0, array.length);
        return result;
    }

    private static double[] vtlnWarpFreq(double vtlnLowCutoff, double vtlnHighCutoff, double lowFreq, double highFreq,
                                        double vtlnWarpFactor, double[] frequencies) {
        double l = vtlnLowCutoff * Math.max(1.0, vtlnWarpFactor);
        double h = vtlnHighCutoff * Math.min(1.0, vtlnWarpFactor);
        double scale = 1.0 / vtlnWarpFactor;
        double Fl = scale * l;  // F(l)
        double Fh = scale * h;  // F(h)

        double[] res = new double[frequencies.length];

        for (int i = 0; i < frequencies.length; i++) {
            boolean outsideLowHighFreq = frequencies[i] < lowFreq || frequencies[i] > highFreq;
            boolean beforeL = frequencies[i] < l;
            boolean beforeH = frequencies[i] < h;
            boolean afterH = frequencies[i] >= h;

            if (afterH) {
                res[i] = highFreq + scaleRight(scale, frequencies[i], highFreq, h);
            } else if (beforeH) {
                res[i] = scale * frequencies[i];
            } else if (beforeL) {
                res[i] = lowFreq + scaleLeft(scale, frequencies[i], lowFreq, l);
            } else if (outsideLowHighFreq) {
                res[i] = frequencies[i];
            }
        }

        return res;
    }

    private static double scaleLeft(double scale, double freq, double lowFreq, double l) {
        return lowFreq + ((scale * l) - lowFreq) / (l - lowFreq) * (freq - lowFreq);
    }

    private static double scaleRight(double scale, double freq, double highFreq, double h) {
        return highFreq + ((scale * h) - highFreq) / (h - highFreq) * (freq - highFreq);
    }

    private static double melScaleScalar(double freq) {
        return 1127.0 * FastMath.log(1.0 + freq / 700.0);
    }

    private static double hertzToMel(double hertz) {
        return 2595 * Math.log10(1 + hertz / 700);
    }

    private static double melToHertz(double mel) {
        return 700 * (Math.pow(10, mel / 2595) - 1);
    }

    private static double[][] padMelEnergies(double[][] melEnergies) {
        int numRows = melEnergies.length;
        int numCols = melEnergies[0].length;

        // Pad melEnergies with an extra column of zeros
        double[][] paddedMelEnergies = new double[numRows][numCols + 1];
        for (int i = 0; i < numRows; i++) {
            paddedMelEnergies[i] = MathArrays.copyOf(melEnergies[i], numCols + 1);
        }

        return paddedMelEnergies;
    }
}
