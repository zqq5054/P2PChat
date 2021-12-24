package com.shawn.fakewechat;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.shawn.fakewechat.app.App;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

public class AddChatActivity extends AppCompatActivity {

    private ImageView imvQrcode;
    private TextView chatAddress;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_chat);
        imvQrcode = findViewById(R.id.qrcode);
        chatAddress = findViewById(R.id.chatAddress);
        String uuid = getIntent().getStringExtra("uuid");
        String url = ((App)getApplication()).createNewClient(uuid);
        chatAddress.setText(url);
        chatAddress.setOnClickListener(v->{
            copyContentToClipboard();
        });
        try {
            Bitmap qrcode = createQRcode(url);
            imvQrcode.setImageBitmap(qrcode);
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }

    public Bitmap createQRcode(String content) throws WriterException {
        int w = 500;
        int h = 500;
        Hashtable<EncodeHintType, String> hints = new Hashtable<EncodeHintType, String>();
        hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
        //图像数据转换，使用了矩阵转换
        BitMatrix bitMatrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, w, h, hints);
        int[] pixels = new int[w * h];
        //下面这里按照二维码的算法，逐个生成二维码的图片，
        //两个for循环是图片横列扫描的结果
        for (int y = 0; y < h; y++)
        {
            for (int x = 0; x < w; x++)
            {
                if (bitMatrix.get(x, y))
                {
                    pixels[y * w + x] = 0xff000000;
                }
                else
                {
                    pixels[y * w + x] = 0xffffffff;
                }
            }
        }
        //生成二维码图片的格式，使用ARGB_8888
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h);
        return bitmap;
    }
    /**
     * 复制内容到剪贴板
     *
     */
    public void copyContentToClipboard() {
        //获取剪贴板管理器：
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        // 创建普通字符型ClipData
        ClipData mClipData = ClipData.newPlainText("Label", chatAddress.getText().toString());
        // 将ClipData内容放到系统剪贴板里。
        cm.setPrimaryClip(mClipData);
        Toast.makeText(this,"已复制",Toast.LENGTH_SHORT).show();
    }
}