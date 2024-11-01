package com.example.morse;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.Manifest;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.google.android.material.textfield.TextInputEditText;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.IOException;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    Button capture_but, morse, szoveg;

    TextInputEditText szoveg_inp;
    TextView adat;
    Bitmap bitmap;

    private HashMap<Character, String> dictionary = new HashMap<>();

    public void initializeDictionary() {
        // Betűk és a hozzájuk tartozó Morse-kódok inicializálása
        dictionary.put('A', ".-");
        dictionary.put('B', "-...");
        dictionary.put('C', "-.-.");
        dictionary.put('D', "-..");
        dictionary.put('E', ".");
        dictionary.put('F', "..-.");
        dictionary.put('G', "--.");
        dictionary.put('H', "....");
        dictionary.put('I', "..");
        dictionary.put('J', ".---");
        dictionary.put('K', "-.-");
        dictionary.put('L', ".-..");
        dictionary.put('M', "--");
        dictionary.put('N', "-.");
        dictionary.put('O', "---");
        dictionary.put('P', ".--.");
        dictionary.put('Q', "--.-");
        dictionary.put('R', ".-.");
        dictionary.put('S', "...");
        dictionary.put('T', "-");
        dictionary.put('U', "..-");
        dictionary.put('V', "...-");
        dictionary.put('W', ".--");
        dictionary.put('X', "-..-");
        dictionary.put('Y', "-.--");
        dictionary.put('Z', "--..");
    }


    private static final int REQUEST_CAMERA_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) ->
        {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        capture_but = findViewById(R.id.capture);
        morse = findViewById(R.id.Morse);
        szoveg = findViewById(R.id.Text);

        szoveg_inp = findViewById(R.id.Szoveg_input);

        adat = findViewById(R.id.text_data);

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                    Manifest.permission.CAMERA
            }, REQUEST_CAMERA_CODE);
        }

        capture_but.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CropImage.activity().setGuidelines(CropImageView.Guidelines.ON).start(MainActivity.this);
            }
        });

        morse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initializeDictionary();
                String morse = stringToMorse(adat.getText().toString(), dictionary);
                Morse(morse, adat.getText().toString());
            }
        });

        szoveg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (szoveg_inp.getText().toString().length() != 0)
                {
                    adat.setText(szoveg_inp.getText().toString().toUpperCase());
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE)
        {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK)
            {
                Uri resultURi = result.getUri();
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(),resultURi);
                    getTextFromImage(bitmap);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private void getTextFromImage(Bitmap pic)
    {
        TextRecognizer recognizer = new TextRecognizer.Builder(this).build();
        if (!recognizer.isOperational())
        {
            Toast.makeText(MainActivity.this, "ERROR!", Toast.LENGTH_SHORT).show();
        }
        else
        {
            Frame frame = new Frame.Builder().setBitmap(pic).build();
            SparseArray<TextBlock> textBlockSparseArray = recognizer.detect(frame);
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i < textBlockSparseArray.size(); i++)
            {
                TextBlock textBlock = textBlockSparseArray.valueAt(i);
                stringBuilder.append(textBlock.getValue());
                stringBuilder.append("\n");
            }
            adat.setText(stringBuilder.toString());
        }
    }

    private  void copyToClipboard(String text)
    {
        ClipboardManager clipboardManager = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Másolt szöveg", text);
        clipboardManager.setPrimaryClip(clip);
        Toast.makeText(MainActivity.this, "Másolva", Toast.LENGTH_SHORT).show();
    }

    private void Flash_Switch(boolean input)
    {
        if (this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH))
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            {
                CameraManager cameraManager = (CameraManager)getSystemService(CAMERA_SERVICE);
                String cameraID = null;
                try {
                    cameraID = cameraManager.getCameraIdList()[0];
                    cameraManager.setTorchMode(cameraID,input);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }
            else
            {
                Camera camera = Camera.open();
                Camera.Parameters p = camera.getParameters();
                p.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                camera.setParameters(p);

                SurfaceTexture texture = new SurfaceTexture(0);
                try {
                    camera.setPreviewTexture(texture);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (input)
                {
                    camera.startPreview();
                }
                else
                {
                    p.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    camera.stopPreview();
                    camera.release();
                }

            }
        }
        else
        {
            Toast.makeText(this, "Nincs flashlight", Toast.LENGTH_SHORT).show();
        }
    }

    private String stringToMorse(String text, HashMap<Character, String> dict)
    {
        String source = text.toUpperCase();
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < source.length(); i++)
        {
            char character = source.charAt(i);
            // Check if the character exists in the dictionary
            if (dict.containsKey(character))
            {
                result.append(dict.get(character)).append(" ");
            } else
            {
                // Handle characters not found in the dictionary
                result.append("");
            }
        }

        return result.toString();
    }

    private void Morse(String text, String eredeti)
    {
        for (int i = 0; i < text.length(); i++)
        {
            if (text.charAt(i) == '.')
            {
                Flash_Switch(true);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                Flash_Switch(false);
                try {
                    Thread.sleep(750);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            else if (text.charAt(i) == '-')
            {
                Flash_Switch(true);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                Flash_Switch(false);
                try {
                    Thread.sleep(750);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        copyToClipboard(eredeti);
    }
}