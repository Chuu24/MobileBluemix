package com.touchesbegan.watson;

import android.media.MediaPlayer;
import android.support.v7.app.ActionBarActivity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.ibm.watson.developer_cloud.language_translation.v2.LanguageTranslation;
import com.ibm.watson.developer_cloud.language_translation.v2.model.TranslationResult;
import com.ibm.watson.developer_cloud.personality_insights.v2.PersonalityInsights;
import com.ibm.watson.developer_cloud.personality_insights.v2.model.Profile;
import com.ibm.watson.developer_cloud.text_to_speech.v1.TextToSpeech;

import org.springframework.http.HttpAuthentication;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpBasicAuthentication;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;

// TODO: Make keyboard appear when focus changes to text entry field, disappear on submit
// TODO: On physical device, keyboard appears but want to submit question check checkbox is
// clicked (currently requires a further click on physical device, or two carriage returns on
// virtual device)

public class MainActivity extends ActionBarActivity {
    ProcessWatsonResponse p = null;
    PersonalityInsights service;
    TextToSpeech textToSpeech;
    LanguageTranslation languageTranslation;

    Profile profile;
    String myProfile;
    String theAnswers;
    String fAnswer;
    MediaPlayer mp = new MediaPlayer();

    private class HttpRequestTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            try {
                final String url = "https://dal09-gateway.watsonplatform.net/instance/582/deepqa/v1/question";

                HttpAuthentication authHeader = new HttpBasicAuthentication(getResources().getString(R.string.username),
                        getResources().getString(R.string.password));
                HttpHeaders requestHeaders = new HttpHeaders();
                requestHeaders.setAuthorization(authHeader);
                requestHeaders.setContentType(new MediaType("application", "xml"));
                requestHeaders.set("X-SyncTimeout", "30");

                TranslationResult translationResult = languageTranslation.translate(params[0], "es", "en");
                String quest = translationResult.getTranslations().get(0).getTranslation();

                String watsonQuery = new String("<question>" +
                        "<questionText>\"" +
                        quest +
                        "\"</questionText>" +
                        "</question>");

                HttpEntity<String> requestEntity = new HttpEntity<>(watsonQuery, requestHeaders);
                RestTemplate restTemplate = new RestTemplate();

                // Add the Jackson and String message converters
                restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
                restTemplate.getMessageConverters().add(new StringHttpMessageConverter());

                // Make the HTTP POST request, marshalling the request to JSON, and the response to String
                ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
                String result = responseEntity.getBody();

                // Parse XML and extract answers
                p = new ProcessWatsonResponse(result);
                if(params[1].equals("Respuesta de Watson")) {
                    profile = service.getProfile(myProfile);
                }else{
                    profile = service.getProfile(params[1]);
                }

                return result;
            } catch (Exception e) {
                Log.e("MainActivity", e.getMessage(), e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(String response) {
            final String BULLET_SYMBOL = "&#8226";

            String allAnswers = new String("Las respuestas de Watson (prioridad descendente):");

            int i = 0;

            for(Map.Entry<Double, String> entry : p.getAnswers().entrySet()) {
                String thisAnswer = entry.getValue();
                if(i == 0){
                    fAnswer = thisAnswer;
                }
                allAnswers = allAnswers + System.getProperty("line.separator") + Html.fromHtml(BULLET_SYMBOL + " " + thisAnswer);
                i++;
                if(i >= 3){
                    break;
                }
            }

            theAnswers = allAnswers;
            new TranslateRequestTask().execute(theAnswers);
            ((TextView) findViewById(R.id.textView4)).setText(profile.toString());
        }
    }

    private class TranslateRequestTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            TranslationResult translationResult = languageTranslation.translate(theAnswers, "en", "es");
            String translation = translationResult.getTranslations().get(0).getTranslation();
            return translation;
        }

        @Override
        protected void onPostExecute(String response) {
            ((TextView) findViewById(R.id.textView)).setText(response);
        }
    }

    EditText editText;

    public boolean onSubmitQuestion(View view) {
        new HttpRequestTask().execute(editText.getText().toString(),
                ((TextView) findViewById(R.id.textView)).getText().toString());
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editText = (EditText) findViewById(R.id.editText);

        myProfile = "Call me Ishmael. Some years ago-never mind how long precisely-having little or no money in my purse, and nothing particular to interest me on shore, I thought I would sail about a little and see the watery part of the world. It is a way I have of driving off the spleen and regulating the circulation. Whenever I find myself growing grim about the mouth; whenever it is a damp, drizzly November in my soul; whenever I find myself involuntarily pausing before coffin warehouses, and bringing up the rear of every funeral I meet; and especially whenever my hypos get such an upper hand of me, that it requires a strong moral principle to prevent me from deliberately stepping into the street, and methodically knocking people's hats off-then, I account it high time to get to sea as soon as I can.";

        service = new PersonalityInsights();
        service.setUsernameAndPassword("45354dea-123b-41d2-bdb2-9e4758ec5aa0", "MuYxHdbBCNpx");
        service.setEndPoint("https://gateway.watsonplatform.net/personality-insights/api");

        textToSpeech = new TextToSpeech();
        textToSpeech.setUsernameAndPassword("9a1cd804-d8c8-4efa-90cc-abc470992a1f", "IyfQLAftR7tu");
        textToSpeech.setEndPoint("https://stream.watsonplatform.net/text-to-speech/api");

        languageTranslation = new LanguageTranslation();
        languageTranslation.setUsernameAndPassword("758e13c6-ad58-4b9e-97e8-1aa933b0f146", "HZfzYpXgfCNx");
        languageTranslation.setEndPoint("https://gateway.watsonplatform.net/language-translation/api");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
