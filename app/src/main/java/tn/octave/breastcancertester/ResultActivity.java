package tn.octave.breastcancertester;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class ResultActivity extends AppCompatActivity {


    TextView txt;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        txt = (TextView) findViewById(R.id.resultValueTxt);
        //Bundle bundle = getIntent().getExtras();
        String st = getIntent().getExtras().getString("result");
        txt.setText(st);

    }
}
