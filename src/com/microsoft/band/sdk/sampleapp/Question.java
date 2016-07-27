package com.microsoft.band.sdk.sampleapp;

import android.content.Context;
import android.util.Log;

import com.microsoft.band.sdk.sampleapp.tileevent.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by shakerk on 7/26/2016.
 */
public class Question {

    public enum Level {
        Easy,
        Medium,
        Hard
    }

    public enum Category {
        All,
        History,
        Science,
        Sports,
        Geography,
        Arts,
        Music,
        TVShows,
        Tech,
        Anime
    }

    private int id;
    private String question;
    private String[] options;
    private String correctAnswer;
    private String message;
    private Level level;
    private Category category;
    private String[] tags;

    private static int counter = 0;
    private static List<Question> questions = new ArrayList<Question>();

    private Question(int id, String question, String[] options, String correctAnswer, String message, Level level, Category category, String[] tags) {
        this.id = id;
        this.question = question;
        this.options = options;
        this.correctAnswer = correctAnswer;
        this.message = message;
        this.level = level;
        this.category = category;
        this.tags = tags;
    }

    public static void generateQuestions(Context context) {
        try {
            InputStream inputStream = context.getResources().openRawResource(R.raw.questions);
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                /*
                Id,Question,Options,Correct Answer,Message,Level,Category,Tags
                1,When did second world war end?,1939; 1945,1945,Second world war ended in 1945.,Easy,History,War
                 */
                String[] input = line.split(",");
                parseQuestionFromInput(input);
            }
        } catch (IOException e) {

        }
        /*questions = {
               new Question(1, "When did second world war end?", new String[] {"1939", "1945"}, 1, "Second world war ended in 1945.", Level.Easy, Category.History, new String[] {"second world war"}),
        };*/
    }
    
    public static Question getAQuestion()
    {
        Random random = new Random(System.currentTimeMillis());
        int randomNumber = random.nextInt(questions.size());
        Log.d("getAQuestion", String.valueOf(randomNumber));
        Question q = questions.get(randomNumber);
        //counter++;
        return q;
    }

    public String getQuestionTitle() {
        return this.question;
    }

    public String[] getOptions() {
        return this.options;
    }

    public String getCorrectAnswer() {
        return this.correctAnswer;
    }

    public String getMessage() {
        return this.message;
    }

    private static void parseQuestionFromInput(String[] input) {
        if (input != null && input.length != 8) {
            Log.e("parseQuestionFromInput", "Invalid input");
        } else {
            String[] options = input[2].split(";");
            String[] tags = input[7].split(";");
            Question q = new Question(Integer.parseInt(input[0]), /* id */
                    input[1],   /* question */
                    options,    /* options */
                    input[3], /* correctAnswer */
                    input[4],   /* message */
                    Level.valueOf(input[5]), /* level */
                    Category.valueOf(input[6]), /* category */
                    tags    /*tags */
            );
            questions.add(q);
        }
    }
}


