package edu.ucla.cs.ndnwhiteboard;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.ArrayList;

/**
 * TODO: document your custom view class.
 */
public class DrawingView extends View {

    private boolean isEraser = false;
    private int currentColor = 0;
    private int viewWidth = 0;

    //drawing path
    private Path drawPath = new Path();
    //drawing and canvas paint
    private Paint drawPaint = new Paint(), canvasPaint = new Paint(Paint.DITHER_FLAG);
    //colors
    //private int[] colors = {Color.BLACK, Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW};
    //Better Colors (All dark)
    private int[] colors = {Color.BLACK, Color.RED, Color.BLUE, 0xFF458B00, 0xFFED9121};
    int num_colors = colors.length;
    //canvas
    private Canvas drawCanvas;
    //canvas bitmap
    private Bitmap canvasBitmap;

    private JSONObject jsonObject = new JSONObject();
    private ArrayList<String> history = new ArrayList<>();
    private ArrayList<PointF> points = new ArrayList<>();
    private WhiteboardActivity activity;

    private boolean initialDrawn = false;

    private String TAG = DrawingView.class.getSimpleName();

    public void setActivity(WhiteboardActivity activity) {
        this.activity = activity;
    }

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupDrawing();
    }

    private void setupDrawing() {
        int dp = 3;
        DisplayMetrics dm = getResources().getDisplayMetrics();
        float pixelWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, dm);
        drawPaint.setColor(Color.BLACK);
        drawPaint.setAntiAlias(true);
        drawPaint.setStrokeWidth(pixelWidth);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        viewWidth = w;
        //Log.d(TAG, "String.valueOf(w): " + String.valueOf(w));
        //Log.d(TAG, "String.valueOf(h)" + String.valueOf(h));
        this.getLayoutParams().height = (int) (6 / 5f * viewWidth);
        canvasBitmap = Bitmap.createBitmap(w, (int) (6 / 5f * w), Bitmap.Config.ARGB_8888);
        drawCanvas = new Canvas(canvasBitmap);
        if (!initialDrawn && h == (int) (6 / 5f * w)) {
            initialDrawn = true;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawBitmap(canvasBitmap, 0, 0, canvasPaint);
        canvas.drawPath(drawPath, drawPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        DecimalFormat df = new DecimalFormat("#.###");
        float touchX = event.getX();
        float touchY = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                drawPath.moveTo(touchX, touchY);
                try {
                    jsonObject = new JSONObject();
                    jsonObject.put("user", activity.username);
                    jsonObject.put("type", (isEraser) ? "eraser" : "pen");
                    if (!isEraser) {
                        jsonObject.put("color", currentColor);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                points.add(new PointF(touchX, touchY));
                break;
            case MotionEvent.ACTION_MOVE:
                drawPath.lineTo(touchX, touchY);
                points.add(new PointF(touchX, touchY));
                int maxCoordSize = (8000 - activity.username.length()) / 18;
                if (points.size() == maxCoordSize) {
                    //Log.d(TAG, "maxCoordSize: " + maxCoordSize);
                    drawCanvas.drawPath(drawPath, drawPaint);
                    drawPath.reset();
                    drawPath.moveTo(touchX, touchY);
                    try {
                        JSONArray coordinates = new JSONArray();
                        for (PointF p : points) {
                            JSONArray ja = new JSONArray();
                            ja.put(df.format(p.x / viewWidth));
                            ja.put(df.format(p.y / viewWidth));
                            coordinates.put(ja);
                        }
                        jsonObject.put("coordinates", coordinates);
                        points.clear();
                        points.add(new PointF(touchX, touchY));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    String jsonString = jsonObject.toString();
                    history.add(jsonString);
                    activity.callback(jsonString);
                }
                break;
            case MotionEvent.ACTION_UP:
                drawCanvas.drawPath(drawPath, drawPaint);
                drawPath.reset();
                try {
                    JSONArray coordinates = new JSONArray();
                    for (PointF p : points) {
                        JSONArray ja = new JSONArray();
                        ja.put(df.format(p.x / viewWidth));
                        ja.put(df.format(p.y / viewWidth));
                        coordinates.put(ja);
                    }
                    jsonObject.put("coordinates", coordinates);
                    points.clear();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                String jsonString = jsonObject.toString();
                history.add(jsonString);
                activity.callback(jsonString);
                break;
            default:
                return false;
        }
        invalidate();
        return true;
    }

    public void setPencil() {
        setColor(currentColor);
    }

    public void setEraser() {
        int dp = 20;
        DisplayMetrics dm = getResources().getDisplayMetrics();
        float pixelWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, dm);
        activity.setButtonColor(Color.WHITE);
        drawPaint.setColor(Color.WHITE);
        drawPaint.setStrokeWidth(pixelWidth);
        isEraser = true;
    }

    private void setColor(int c) {
        int dp = 3;
        DisplayMetrics dm = getResources().getDisplayMetrics();
        float pixelWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, dm);
        currentColor = c;
        activity.setButtonColor(colors[currentColor]);
        drawPaint.setColor(colors[currentColor]);
        drawPaint.setStrokeWidth(pixelWidth);
        isEraser = false;
    }

    public void incrementColor() {
        if (!isEraser && ++currentColor > num_colors-1) {
            currentColor = 0;
        }
        setColor(currentColor);
    }

    public void undo() {
        if (history.isEmpty()) {
            Toast.makeText(activity, "No more history", Toast.LENGTH_SHORT).show();
            return;
        }
        for (int i = history.size() - 1; i >= 0; i--) {
            if (history.get(i).contains("\"user\":\"" + activity.username + "\"")) {
                history.remove(i);
                try {
                    jsonObject = new JSONObject();
                    jsonObject.put("user", activity.username);
                    jsonObject.put("type", "undo");
                    activity.callback(jsonObject.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
        drawCanvas.drawColor(Color.WHITE);
        invalidate();
        for (String string : history) {
            parseJSON(string, false);
        }
    }

    public void clear() {
        history.clear();
        drawCanvas.drawColor(Color.WHITE);
        invalidate();
        try {
            jsonObject = new JSONObject();
            jsonObject.put("user", activity.username);
            jsonObject.put("type", "clear");
            activity.callback(jsonObject.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void callback(String string) {
        parseJSON(string, true);
    }

    public void parseJSON(String string, boolean addToHistory) {
        try {
            JSONObject jsonObject = new JSONObject(string);
            try {
                int colorBefore = currentColor;
                boolean isEraserBefore = isEraser;
                String type = jsonObject.get("type").toString();
                if (type.equals("pen")) {
                    int color = jsonObject.getInt("color");
                    setColor(color);
                } else if (type.equals("eraser")) {
                    setEraser();
                } else if (type.equals("undo")) {
                    if (history.isEmpty()) {
                        return;
                    }
                    String userStr = jsonObject.getString("user");
                    for (int i = history.size() - 1; i >= 0; i--) {
                        if (history.get(i).contains("\"user\":\"" + userStr + "\"")) {
                            history.remove(i);
                            break;
                        }
                    }
                    drawCanvas.drawColor(Color.WHITE);
                    invalidate();
                    for (String str : history) {
                        parseJSON(str, false);
                    }
                } else if (type.equals("clear")) {
                    history.clear();
                    drawCanvas.drawColor(Color.WHITE);
                    invalidate();
                } else {
                    throw new JSONException("Unrecognized string: " + string);
                }
                if (type.equals("pen") || type.equals("eraser")) {
                    JSONArray coordinates = jsonObject.getJSONArray("coordinates");
                    JSONArray startPoint = coordinates.getJSONArray(0);
                    Path drawPath = new Path();
                    float touchX = (float) startPoint.getDouble(0) * viewWidth;
                    float touchY = (float) startPoint.getDouble(1) * viewWidth;
                    drawPath.moveTo(touchX, touchY);
                    for (int i = 1; i < coordinates.length(); i++) {
                        JSONArray point = coordinates.getJSONArray(i);
                        float x = (float) point.getDouble(0) * viewWidth;
                        float y = (float) point.getDouble(1) * viewWidth;
                        drawPath.lineTo(x, y);
                    }
                    drawCanvas.drawPath(drawPath, drawPaint);
                    invalidate();
                    drawPath.reset();
                    if (addToHistory) {
                        history.add(string);
                    }
                    if (isEraserBefore) {
                        setEraser();
                    } else {
                        setColor(colorBefore);
                    }
                }
            } catch (JSONException e) {
                Log.d(TAG, "JSON string error: " + string);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}
