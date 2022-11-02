package vikas.mrzscanner;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

public class ResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        TextView textView =(TextView) findViewById(R.id.textView);
        textView.setText(getIntent().getStringExtra("MrzNumber"));

    }
}