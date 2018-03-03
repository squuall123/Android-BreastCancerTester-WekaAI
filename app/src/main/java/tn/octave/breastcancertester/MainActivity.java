package tn.octave.breastcancertester;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
//Weka imports

import com.obsez.android.lib.filechooser.ChooserDialog;

import java.io.File;
import java.util.ArrayList;

import weka.classifiers.Evaluation;
import weka.core.Instances;
import weka.filters.Filter;
import weka.classifiers.functions.MultilayerPerceptron;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.FastVector;
import weka.core.Instance;
import weka.filters.unsupervised.instance.RemovePercentage;
import weka.core.converters.ConverterUtils.DataSource;

public class MainActivity extends AppCompatActivity {
    private Context mContext=MainActivity.this;
    private static final int REQUEST = 112;
    LinearLayout mRlayout;
    LinearLayout.LayoutParams mRparams;
    ArrayList<EditText> mList;
    Button processing;
    Intent intent;
    Button choose;
    String path = "/sdcard/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Check for permission
        if (Build.VERSION.SDK_INT >= 23) {
            String[] PERMISSIONS = {android.Manifest.permission.READ_EXTERNAL_STORAGE};
            if (!hasPermissions(mContext, PERMISSIONS)) {
                ActivityCompat.requestPermissions((Activity) mContext, PERMISSIONS, REQUEST );
            } else {
                onResume();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setContentView(R.layout.activity_main);
        intent = new Intent(this, ResultActivity.class);
        mRlayout = (LinearLayout) findViewById(R.id.layout);
        mRparams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        choose = (Button)findViewById(R.id.chooser);


        choose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new ChooserDialog().with(mContext)
                        .withStartFile(path)
                        .withChosenListener(new ChooserDialog.Result() {
                            @Override
                            public void onChoosePath(String path, File pathFile) {
                                Toast.makeText(MainActivity.this, "FILE: " + path, Toast.LENGTH_SHORT).show();
                                choose.setVisibility(View.INVISIBLE);
                                try {
                                    readFile(path);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        })
                        .build()
                        .show();
            }
        });



    }

    private static boolean hasPermissions(Context context, String... permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    public void readFile(String path) throws Exception{
        /**
         * Charger le fichier contenant la base de données ARFF dans la mémoire
         */

        /* Lécture de l'archive ARFF*/

        DataSource oDataSet = new DataSource(path);
        System.out.println("size = "+ oDataSet.getDataSet().numAttributes());
        System.out.println("name = "+ oDataSet.getDataSet().relationName());
        /* Déclaration d'un objet Instances appelé iDataSet */
        final Instances iDataSet = oDataSet.getDataSet();

        /* Sélection des attributs*/
        iDataSet.setClassIndex(iDataSet.numAttributes() - 1);
        final FastVector attributes = new FastVector();
        mList = new ArrayList<EditText>();

        for (int i = 0; i<iDataSet.numAttributes() - 1;i++){
            //System.out.println("Attribute name = " + iDataSet.attribute(i).name());
            EditText myEditText = new EditText(this.mContext);
            myEditText.setLayoutParams(mRparams);
            myEditText.setId(i);
            myEditText.setHint(iDataSet.attribute(i).name());
            myEditText.setInputType(InputType.TYPE_CLASS_NUMBER);

            mRlayout.addView(myEditText);
            mList.add(myEditText);
            /* Déclaration d'un vecteur d'attributs */
            attributes.addElement(new Attribute(iDataSet.attribute(i).name()));

        }

        /* Déclaration d'un attribut de classe avec ses valeurs possibles */
        System.out.println("class size = "+ iDataSet.classAttribute().numValues());
        FastVector fvClassVal = new FastVector(iDataSet.classAttribute().numValues());
        for (int i = 0;i<iDataSet.classAttribute().numValues();i++){
            //System.out.println("class name = "+ iDataSet.classAttribute().value(i));
            fvClassVal.addElement(iDataSet.classAttribute().value(i));
        }
        attributes.addElement(new Attribute("class", fvClassVal));

        // Ajout du bouton de traitement
        processing = new Button(this.mContext);
        processing.setText("Detect");
        mRlayout.addView(processing);

        Log.d("size = ", mList.size()+"");
        for (int i=0; i<mList.size();i++){
            Log.d("id = ", mList.get(i).getHint().toString());
        }

         /* Déclaration d'un objet Instances appelé iSingleData */
        final Instances iSingleData = new Instances("Nouvel échantillon", attributes, 1);

        /* Sélection d'attribut de classe */
        iSingleData.setClassIndex(iSingleData.numAttributes() - 1);

        /* Charger les valeurs des attributs de l'échantillon à classer */
        final double[] values = new double[iSingleData.numAttributes()];

        processing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                for (int i=0; i<mList.size();i++){
                    Log.d("input = ", mList.get(i).getText().toString());
                    values[i]= Double.valueOf(mList.get(i).getText().toString());
                }

                Instance ech = new DenseInstance(1.0, values);

                /* Ajouter un exemple dans l'instance iSingleData */
                iSingleData.add(ech);

                /* Définir l'attribut de classe comme valeur inconnue (?) */
                iSingleData.get(0).setClassMissing();

                 /* Imprime le contenu de l'instance iSingleData sur l'écran */
                //System.out.println(iSingleData);


                /**
                 * Séparation des données en sous-ensembles pour le process et le test
                 */

                double testPercentage = 10.0;

                /* Sous-ensemble du process */
                RemovePercentage rp = new RemovePercentage();
                rp.setPercentage(testPercentage);
                try {
                    rp.setInputFormat(iDataSet);
                    Instances iProcess = Filter.useFilter(iDataSet, rp);


                    /* Sous-ensemble de test */
                    rp.setInputFormat(iDataSet);
                    rp.setInvertSelection(true);
                    Instances iTest = Filter.useFilter(iDataSet, rp);

                    System.out.println("Totales des instances \t"+iDataSet.numInstances());
                    System.out.println("Instance du process \t"+iProcess.numInstances());
                    System.out.println("Instance du test: \t"+iTest.numInstances());


                    /**
                     * Déclaration de la ANN (Artificial Neural Network)
                     */
                    MultilayerPerceptron ann = new MultilayerPerceptron();

                    /* Entrainement de la ANN*/
                    ann.buildClassifier(iProcess);
                    weka.core.SerializationHelper.write("/storage/emulated/0/test/model.model", ann);

                    /* Test de la ANN */
                    Evaluation eval = new Evaluation(iProcess);
                    eval.evaluateModel(ann, iTest);

                    /* Résultats de la classification */
                    for (int i = 0; i < iSingleData.numInstances(); i++) {
                        double pred = ann.classifyInstance(iSingleData.instance(i));
                        System.out.println("Résultat : " + iSingleData.classAttribute().value((int) pred));
                        Toast.makeText(mContext, iSingleData.classAttribute().value((int) pred), Toast.LENGTH_SHORT).show();
                        intent.putExtra("result",iSingleData.classAttribute().value((int) pred));
                        startActivity(intent);

                        //if (!iSingleData.classAttribute().value((int) pred).equals(null))
                        //showResult(iSingleData.classAttribute().value((int) pred));
                    }




                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });

    }


}
