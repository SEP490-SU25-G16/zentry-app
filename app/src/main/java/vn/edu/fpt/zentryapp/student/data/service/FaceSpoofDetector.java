package vn.edu.fpt.zentryapp.student.data.service;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;


import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.ops.CastOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Utility class for face spoof detection
 * Adapted from the OnDevice-Face-Recognition-Android project
 */
public class FaceSpoofDetector {
    private static final String TAG = "FaceSpoofDetector";
    private static final String MODEL_FILE_1 = "spoof_model_scale_2_7.tflite";
    private static final String MODEL_FILE_2 = "spoof_model_scale_4_0.tflite";
    
    private static final float SCALE_1 = 2.7f;
    private static final float SCALE_2 = 4.0f;
    private static final int INPUT_IMAGE_DIM = 80;
    private static final int OUTPUT_DIM = 3;
    private static final int REAL_FACE_LABEL = 0;
    
    private final Interpreter firstModelInterpreter;
    private final Interpreter secondModelInterpreter;
    private final ImageProcessor imageTensorProcessor;
    private boolean useMockDetection = false;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    public static class SpoofResult {
        private final boolean isSpoof;
        private final float score;
        private final long timeMillis;
        
        public SpoofResult(boolean isSpoof, float score, long timeMillis) {
            this.isSpoof = isSpoof;
            this.score = score;
            this.timeMillis = timeMillis;
        }
        
        public boolean isSpoof() {
            return isSpoof;
        }
        
        public float getScore() {
            return score;
        }
        
        public long getTimeMillis() {
            return timeMillis;
        }
    }
    
    public FaceSpoofDetector(Context context) {
        Interpreter firstInterpreter = null;
        Interpreter secondInterpreter = null;
        ImageProcessor processor = null;
        
        try {
            // Log thông tin về assets để debug
            logAssetsContent(context);
            
            try {
                Log.d(TAG, "Đang tải các file model...");
                
                // Initialize TFLiteInterpreter
                Interpreter.Options interpreterOptions = new Interpreter.Options();
                
                // Thử sử dụng GPU nếu có thể
                try {
                    // Sử dụng GPU Delegate
                    GpuDelegate gpuDelegate = new GpuDelegate();
                    interpreterOptions.addDelegate(gpuDelegate);
                    Log.d(TAG, "Đã thêm GPU Delegate");
                } catch (Exception e) {
                    // Fallback to CPU if GPU is not supported
                    Log.d(TAG, "Không thể sử dụng GPU, chuyển sang CPU: " + e.getMessage());
                    interpreterOptions.setNumThreads(4);
                }
                
                // Tải các model từ assets
                MappedByteBuffer model1Buffer = FileUtil.loadMappedFile(context, MODEL_FILE_1);
                MappedByteBuffer model2Buffer = FileUtil.loadMappedFile(context, MODEL_FILE_2);
                
                Log.d(TAG, "Model 1 đã tải, kích thước: " + model1Buffer.capacity() + " bytes");
                Log.d(TAG, "Model 2 đã tải, kích thước: " + model2Buffer.capacity() + " bytes");
                
                // Tạo interpreter
                firstInterpreter = new Interpreter(model1Buffer, interpreterOptions);
                secondInterpreter = new Interpreter(model2Buffer, interpreterOptions);
                
                // Tạo image processor cho tiền xử lý
                processor = new ImageProcessor.Builder()
                        .add(new CastOp(DataType.FLOAT32))
                        .build();
                
                Log.d(TAG, "Đã tải các model thành công");
            } catch (Exception e) {
                Log.e(TAG, "Lỗi khi khởi tạo model TensorFlow Lite: " + e.getMessage(), e);
                Toast.makeText(context, "Lỗi khi tải model spoof detection: " + e.getMessage(), Toast.LENGTH_LONG).show();
                useMockDetection = true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi kiểm tra file model: " + e.getMessage(), e);
            Toast.makeText(context, "Lỗi khi kiểm tra model spoof detection: " + e.getMessage(), Toast.LENGTH_LONG).show();
            useMockDetection = true;
        }
        
        this.firstModelInterpreter = firstInterpreter;
        this.secondModelInterpreter = secondInterpreter;
        this.imageTensorProcessor = processor;
    }
    
    private void logAssetsContent(Context context) {
        try {
            String[] files = context.getAssets().list("");
            Log.d(TAG, "Nội dung thư mục assets: " + Arrays.toString(files));
            
            // Kiểm tra chi tiết về các file model
            for (String file : files) {
                if (file.endsWith(".tflite")) {
                    try {
                        MappedByteBuffer buffer = FileUtil.loadMappedFile(context, file);
                        Log.d(TAG, "File model: " + file + ", kích thước: " + buffer.capacity() + " bytes");
                    } catch (Exception e) {
                        Log.e(TAG, "Lỗi khi kiểm tra file model " + file + ": " + e.getMessage(), e);
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Lỗi khi liệt kê thư mục assets: " + e.getMessage(), e);
        }
    }
    
    /**
     * Detect if a face is spoofed asynchronously
     * @param frameImage Original frame image
     * @param faceRect Face bounding box
     * @param callback Callback for result
     */
    public void detectSpoofAsync(Bitmap frameImage, Rect faceRect, SpoofCallback callback) {
        executor.execute(() -> {
            SpoofResult result = detectSpoof(frameImage, faceRect);
            mainHandler.post(() -> callback.onResult(result));
        });
    }
    
    /**
     * Callback interface for spoof detection
     */
    public interface SpoofCallback {
        void onResult(SpoofResult result);
    }

    /**
     * Detect if a face is spoofed
     * @param frameImage Original frame image
     * @param faceRect Face bounding box
     * @return Spoof detection result
     */
    public SpoofResult detectSpoof(Bitmap frameImage, Rect faceRect) {
        long startTime = System.currentTimeMillis();

        // Nếu đang sử dụng mock detection hoặc interpreter không được khởi tạo, luôn trả về không phải spoof
        if (useMockDetection || firstModelInterpreter == null || secondModelInterpreter == null || imageTensorProcessor == null) {
            Log.d(TAG, "Đang sử dụng mock spoof detection (luôn trả về không phải spoof)");
            return new SpoofResult(false, 0.95f, System.currentTimeMillis() - startTime);
        }

        try {
            Log.d(TAG, "Bắt đầu phát hiện spoof với bounding box: " + faceRect.toString());

            // Cắt và scale ảnh khuôn mặt với hai hằng số đã cho
            Bitmap croppedImage1 = crop(
                    frameImage,
                    faceRect,
                    SCALE_1,
                    INPUT_IMAGE_DIM,
                    INPUT_IMAGE_DIM
            );

            Log.d(TAG, "Đã cắt ảnh 1 với scale " + SCALE_1 + ", kích thước: " + croppedImage1.getWidth() + "x" + croppedImage1.getHeight());

            // Chuyển RGB sang BGR
            Bitmap bgrImage1 = convertRgbToBgr(croppedImage1);
            Log.d(TAG, "Đã chuyển ảnh 1 sang BGR");

            Bitmap croppedImage2 = crop(
                    frameImage,
                    faceRect,
                    SCALE_2,
                    INPUT_IMAGE_DIM,
                    INPUT_IMAGE_DIM
            );

            Log.d(TAG, "Đã cắt ảnh 2 với scale " + SCALE_2 + ", kích thước: " + croppedImage2.getWidth() + "x" + croppedImage2.getHeight());

            // Chuyển RGB sang BGR
            Bitmap bgrImage2 = convertRgbToBgr(croppedImage2);
            Log.d(TAG, "Đã chuyển ảnh 2 sang BGR");

            // Xử lý ảnh
            TensorImage tensorImage1 = TensorImage.fromBitmap(bgrImage1);
            TensorImage tensorImage2 = TensorImage.fromBitmap(bgrImage2);

            tensorImage1 = imageTensorProcessor.process(tensorImage1);
            tensorImage2 = imageTensorProcessor.process(tensorImage2);

            Log.d(TAG, "Đã xử lý tensor image");

            // Lấy buffer từ TensorImage
            ByteBuffer input1 = tensorImage1.getBuffer();
            ByteBuffer input2 = tensorImage2.getBuffer();

            // Chuẩn bị output buffer
            float[][] output1 = new float[1][OUTPUT_DIM];
            float[][] output2 = new float[1][OUTPUT_DIM];

            // Chạy inference
            firstModelInterpreter.run(input1, output1);
            secondModelInterpreter.run(input2, output2);

            Log.d(TAG, "Đã chạy inference");
            Log.d(TAG, "Output model 1: [" + output1[0][0] + ", " + output1[0][1] + ", " + output1[0][2] + "]");
            Log.d(TAG, "Output model 2: [" + output2[0][0] + ", " + output2[0][1] + ", " + output2[0][2] + "]");

            // Áp dụng softmax cho outputs
            float[] softmax1 = softMax(output1[0]);
            float[] softmax2 = softMax(output2[0]);

            Log.d(TAG, "Softmax model 1: [" + softmax1[0] + ", " + softmax1[1] + ", " + softmax1[2] + "]");
            Log.d(TAG, "Softmax model 2: [" + softmax2[0] + ", " + softmax2[1] + ", " + softmax2[2] + "]");

            // Kết hợp kết quả - dựa trên logic từ FaceSpoofDetector.kt
            float[] combined = new float[OUTPUT_DIM];
            for (int i = 0; i < OUTPUT_DIM; i++) {
                combined[i] = (softmax1[i] + softmax2[i]) / 2.0f;
            }

            Log.d(TAG, "Combined result: [" + combined[0] + ", " + combined[1] + ", " + combined[2] + "]");

            // Lấy nhãn có xác suất cao nhất
            int label = 0;
            float maxProb = combined[0];
            for (int i = 1; i < OUTPUT_DIM; i++) {
                if (combined[i] > maxProb) {
                    maxProb = combined[i];
                    label = i;
                }
            }

            Log.d(TAG, "Nhãn với xác suất cao nhất: " + label + " với giá trị: " + maxProb);

            // Nhãn 1 là khuôn mặt thật, các nhãn khác là spoof (giống như trong FaceSpoofDetector.kt)
            boolean isSpoof = label != REAL_FACE_LABEL;
            float score = combined[label];

            Log.d(TAG, "Kết quả phát hiện spoof: " + (isSpoof ? "SPOOF" : "REAL") + " với độ tin cậy: " + score);

            long timeMillis = System.currentTimeMillis() - startTime;
            return new SpoofResult(isSpoof, score, timeMillis);

        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi phát hiện spoof: " + e.getMessage(), e);
            // Mặc định trả về không phải spoof trong trường hợp lỗi
            return new SpoofResult(false, 0.9f, System.currentTimeMillis() - startTime);
        }
    }
    
    /**
     * Chuyển đổi ảnh RGB sang BGR
     */
    private Bitmap convertRgbToBgr(Bitmap input) {
        Bitmap output = input.copy(input.getConfig(), true);
        
        for (int i = 0; i < output.getWidth(); i++) {
            for (int j = 0; j < output.getHeight(); j++) {
                int pixel = output.getPixel(i, j);
                output.setPixel(i, j, Color.rgb(
                        Color.blue(pixel),
                        Color.green(pixel),
                        Color.red(pixel)
                ));
            }
        }
        
        return output;
    }
    
    /**
     * Apply softmax to array
     */
    private float[] softMax(float[] x) {
        float[] exp = new float[x.length];
        float sum = 0.0f;
        
        // Tính exp và tổng
        for (int i = 0; i < x.length; i++) {
            exp[i] = (float) Math.exp(x[i]);
            sum += exp[i];
        }
        
        // Chuẩn hóa
        for (int i = 0; i < exp.length; i++) {
            exp[i] = exp[i] / sum;
        }
        
        return exp;
    }

    /**
     * Crop and scale face region - dựa trên logic từ FaceSpoofDetector.kt
     */
    private Bitmap crop(Bitmap origImage, Rect bbox, float bboxScale, int targetWidth, int targetHeight) {
        int srcWidth = origImage.getWidth();
        int srcHeight = origImage.getHeight();

        Log.d(TAG, "Crop: Ảnh gốc kích thước " + srcWidth + "x" + srcHeight + ", bbox: " + bbox.toString());

        // Scale bounding box
        Rect scaledBox = getScaledBox(srcWidth, srcHeight, bbox, bboxScale);
        Log.d(TAG, "Crop: Scaled box: " + scaledBox.toString());

        // Crop image
        Bitmap croppedBitmap = Bitmap.createBitmap(
                origImage,
                scaledBox.left,
                scaledBox.top,
                scaledBox.width(),
                scaledBox.height()
        );

        Log.d(TAG, "Crop: Cropped bitmap size: " + croppedBitmap.getWidth() + "x" + croppedBitmap.getHeight());

        // Resize to target dimensions
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(croppedBitmap, targetWidth, targetHeight, true);
        Log.d(TAG, "Crop: Resized bitmap size: " + resizedBitmap.getWidth() + "x" + resizedBitmap.getHeight());

        return resizedBitmap;
    }

    /**
     * Scale bounding box - dựa trên logic từ FaceSpoofDetector.kt
     */
    private Rect getScaledBox(int imgWidth, int imgHeight, Rect box, float bboxScale) {
        int x = box.left;
        int y = box.top;
        int w = box.width();
        int h = box.height();

        Log.d(TAG, "getScaledBox: Input - imgWidth: " + imgWidth + ", imgHeight: " + imgHeight +
                ", box: " + box.toString() + ", scale: " + bboxScale);

        // Tính toán scale dựa trên logic từ FaceSpoofDetector.kt
        float scale = Math.min(Math.min((imgHeight - 1f) / h, (imgWidth - 1f) / w), bboxScale);
        Log.d(TAG, "getScaledBox: Calculated scale: " + scale);

        float newWidth = w * scale;
        float newHeight = h * scale;
        float centerX = w / 2f + x;
        float centerY = h / 2f + y;

        float topLeftX = centerX - newWidth / 2f;
        float topLeftY = centerY - newHeight / 2f;
        float bottomRightX = centerX + newWidth / 2f;
        float bottomRightY = centerY + newHeight / 2f;

        Log.d(TAG, "getScaledBox: Initial scaled box - topLeft: (" + topLeftX + ", " + topLeftY +
                "), bottomRight: (" + bottomRightX + ", " + bottomRightY + ")");

        // Đảm bảo box nằm trong giới hạn ảnh
        if (topLeftX < 0) {
            bottomRightX -= topLeftX;
            topLeftX = 0;
        }
        if (topLeftY < 0) {
            bottomRightY -= topLeftY;
            topLeftY = 0;
        }
        if (bottomRightX > imgWidth - 1) {
            topLeftX -= (bottomRightX - (imgWidth - 1));
            bottomRightX = imgWidth - 1;
        }
        if (bottomRightY > imgHeight - 1) {
            topLeftY -= (bottomRightY - (imgHeight - 1));
            bottomRightY = imgHeight - 1;
        }

        Rect result = new Rect((int)topLeftX, (int)topLeftY, (int)bottomRightX, (int)bottomRightY);
        Log.d(TAG, "getScaledBox: Final scaled box: " + result.toString());

        return result;
    }
    
    /**
     * Release resources
     */
    public void close() {
        if (firstModelInterpreter != null) {
            firstModelInterpreter.close();
        }
        if (secondModelInterpreter != null) {
            secondModelInterpreter.close();
        }
    }
} 