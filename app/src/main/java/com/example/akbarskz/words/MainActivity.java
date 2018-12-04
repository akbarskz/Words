package com.example.akbarskz.words;

import android.content.ClipData;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.akbarskz.words.Model.Game;
import com.example.akbarskz.words.Model.Theme;
import com.example.akbarskz.words.Model.Word;
import com.example.akbarskz.words.Utils.Pair;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MainActivity extends AppCompatActivity {

    public final int DRAG_OVER_SIZE_CHANGE = 4;
    private final String CHARACTER_PLACEHOLDER = "  ";


    private LinearLayout targetLayout, sourceLayout;
    private Button checkButton;
    private TextView themeText, translationText;
    private ImageView playImage;

    private MediaPlayer mPlayer;

    private Map<Integer, TextView> targetChars;

    private Game game;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        targetLayout = findViewById(R.id.target);
        sourceLayout = findViewById(R.id.source);
        checkButton = findViewById(R.id.check);
        playImage = findViewById(R.id.play);
        themeText = findViewById(R.id.theme);
        translationText = findViewById(R.id.translation);

        targetChars = new TreeMap<>();

        // Инициализируем обработчик клика по кнопке проверки
        checkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Выводим результат проверки
                if (checkResult()) {
                    Toast.makeText(MainActivity.this, "Правильно!", Toast.LENGTH_SHORT).show();

                    // Загружаем новое слово с задержкой 2 сек
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            nextWord();
                        }
                    }, 2000);

                } else {
                    Toast.makeText(MainActivity.this, "Попробуй еще!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        playImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sayCurrentWord();
            }
        });

        String jsonString = getWordsJsonFromResources();
        game = new Game(loadThemes(jsonString));

        nextWord();
    }

    /**
     * Загрузка тем из json
     *
     * @param jsonString
     * @return
     */
    private Theme[] loadThemes(String jsonString) {
        Theme[] allThemes = new Gson().fromJson(jsonString, Theme[].class);
        List<Theme> activeThemes = new ArrayList<>();
        for (Theme theme : allThemes) {
            if (theme.getIsActive()) {
                activeThemes.add(theme);
            }
        }
        return activeThemes.toArray(new Theme[0]);
    }

    /**
     * Получение json из ресурсов
     *
     * @return Текст json
     */
    private String getWordsJsonFromResources() {
        InputStream is = getResources().openRawResource(R.raw.words);
        Writer writer = new StringWriter();
        char[] buffer = new char[1024];
        try {
            Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            int n;
            while ((n = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, n);
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return writer.toString();
    }

    /**
     * Инициализация следующего слова
     *
     */
    private void nextWord() {
        Pair<Theme, Word> nextWord = game.nextWord();

        Theme theme = nextWord.getFirst();
        Word word = nextWord.getSecond();

        themeText.setText(theme.getTheme());
        translationText.setText(word.getWordRu());

        // Загрузчик представления
        LayoutInflater mInflater = LayoutInflater.from(MainActivity.this);

        // Получаем маску слова
        ArrayList<Character> wordMask = game.getWordMask();

        // Очищаем все имеющиеся элементы слова
        targetLayout.removeAllViews();
        targetChars.clear();

        // Добавляем места для букв слова
        for (int i = 0; i < wordMask.size(); i++) {
            Character character = wordMask.get(i);

            TextView target = (TextView) mInflater.inflate(
                    character.charValue() == 'X'
                            ? R.layout.item_character
                            : R.layout.special_character,
                    targetLayout, false);
            target.setTag(i);

            if (character.charValue() == 'X') {
                target.setText(CHARACTER_PLACEHOLDER);
                target.setOnDragListener(dragListener);
                target.setOnLongClickListener(longClickListener);
                target.setOnClickListener(targetClickListener);
            } else {
                target.setText(character + "");
            }

            targetLayout.addView(target);

            targetChars.put(i, target);
        }


        // Получаем список букв в произвольном порядке
        ArrayList<Character> characters = game.getRandomCharacters();

        // Очищаем все имеющиеся символы
        sourceLayout.removeAllViews();

        // Добавляем места для символов
        for (Character character : characters) {
            TextView source = (TextView) mInflater
                    .inflate(R.layout.item_character, sourceLayout, false);
            source.setText(character + "");
            source.setOnDragListener(dragListener);
            source.setOnLongClickListener(longClickListener);
            source.setOnClickListener(sourceClickListener);
            sourceLayout.addView(source);
        }

        // Обновяем видимость кнопки проверки слова
        updateCheckButtonVisibility();

        // Произносим слово
        sayCurrentWord();
    }

    /**
     * Обработчик длительного нажатия.
     * При длительном нажатии инициируется перенос
     */
    View.OnLongClickListener longClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            TextView source = (TextView) v;
            if (!canDrag(source)) return false;

            ClipData data = ClipData.newPlainText("", "");
            View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(source);
            source.startDrag(data, shadowBuilder, source, 0);
            return true;
        }
    };

    /**
     * Обработчик нажатия на исходные символы
     * Символ на который нажал пользователь переносится на первую свободную позицию слова
     */
    View.OnClickListener sourceClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            TextView source = (TextView) v;
            if (!canDrag(source)) return;

            int targetCount = targetChars.size();
            for (int i = 0; i < targetCount; i++) {
                TextView target = targetChars.get(i);
                if (canDrop(source, target)) {
                    moveCharacter(source, target);
                    break;
                }
            }
        }
    };

    /**
     * Обработчик нажатия на символы слова.
     * Символ на который нажал пользователь переносится на свободную позицию исходного списка символов
     */
    View.OnClickListener targetClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            TextView source = (TextView) v;
            if (!canDrag(source)) return;

            int sourceCount = sourceLayout.getChildCount();
            for (int i = 0; i < sourceCount; i++) {
                TextView target = (TextView) sourceLayout.getChildAt(i);
                if (canDrop(source, target)) {
                    moveCharacter(source, target);
                    break;
                }
            }
        }
    };

    /**
     * Обработчик перемещения символа
     */
    View.OnDragListener dragListener = new View.OnDragListener() {
        @Override
        public boolean onDrag(View v, DragEvent event) {
            int dragEvent = event.getAction();
            // Приемник
            final TextView target = (TextView) v;
            // Переносимый символ
            final TextView source = (TextView) event.getLocalState();

            switch (dragEvent) {
                case DragEvent.ACTION_DRAG_STARTED:
                    break;
                case DragEvent.ACTION_DRAG_ENDED:
                    break;
                case DragEvent.ACTION_DRAG_ENTERED:
                    if (canDrop(source, target)) {
                        increaseSizeWhenDragEntered(target);
                    }
                    break;
                case DragEvent.ACTION_DRAG_EXITED:
                    if (canDrop(source, target)) {
                        decreaseSizeWhenDragExited(target);
                    }
                    break;
                case DragEvent.ACTION_DROP:
                    if (!canDrop(source, target)) break;

                    // Восстанавливаем размер приемника
                    decreaseSizeWhenDragExited(target);

                    // Переносим символ
                    moveCharacter(source, target);

                    break;
            }

            return true;
        }
    };

    /**
     * Перенос символа
     *
     * @param source Источник
     * @param target Приемник
     */
    private void moveCharacter(TextView source, TextView target) {
        // Меняем местами символы
        String character = target.getText().toString();
        target.setText(source.getText());
        source.setText(character);

        // Проверяем закончено ли построение слова
        updateCheckButtonVisibility();
    }

    /**
     * Определение возможности перетаскивания
     *
     * @param source Источник
     * @return Признак возможности переноса
     */
    private boolean canDrag(TextView source) {
        // Переносить можно только не пустой символ
        return !source.getText().equals(CHARACTER_PLACEHOLDER);
    }

    /**
     * Определение возможности переноса
     *
     * @param source Источник
     * @param target Приемник
     * @return Признак возможности переноса
     */
    private boolean canDrop(TextView source, TextView target) {
        // Переносить можно только на пустую позицию
        // Переносить символы
        return !source.getText().equals(CHARACTER_PLACEHOLDER) &&
                target.getText().equals(CHARACTER_PLACEHOLDER) &&
                (source.getParent().equals(targetLayout) || target.getParent().equals(targetLayout));
    }

    /**
     * Уменьшение размера рамки символа при выходе
     *
     * @param target Символ
     */
    private void decreaseSizeWhenDragExited(TextView target) {
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) target.getLayoutParams();
        int offset = sizeInDp(DRAG_OVER_SIZE_CHANGE);
        layoutParams.setMargins(
                layoutParams.leftMargin + offset,
                layoutParams.topMargin + offset,
                layoutParams.rightMargin + offset,
                layoutParams.bottomMargin + offset);
        target.setPadding(
                target.getPaddingLeft() - offset,
                target.getPaddingTop() - offset,
                target.getPaddingRight() - offset,
                target.getPaddingBottom() - offset);
    }

    /**
     * Увеличение размера рамки символа при входе
     *
     * @param target Символ
     */
    private void increaseSizeWhenDragEntered(TextView target) {
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) target.getLayoutParams();
        int offset = sizeInDp(DRAG_OVER_SIZE_CHANGE);
        layoutParams.setMargins(
                layoutParams.leftMargin - offset,
                layoutParams.topMargin - offset,
                layoutParams.rightMargin - offset,
                layoutParams.bottomMargin - offset);
        target.setPadding(
                target.getPaddingLeft() + offset,
                target.getPaddingTop() + offset,
                target.getPaddingRight() + offset,
                target.getPaddingBottom() + offset);
    }

    /**
     * Перевод размера в dp
     *
     * @param value Размер в dp
     * @return Итоговый размер
     */
    private int sizeInDp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }

    /**
     * Обновляем видимость кнопки проверки.
     * Кнопка видима, если построение слова завершено
     */
    private void updateCheckButtonVisibility() {
        int visibility;
        if (isWordComplete())
            visibility = View.VISIBLE;
        else
            visibility = View.INVISIBLE;

        if (checkButton.getVisibility() != visibility) {
            checkButton.setVisibility(visibility);
        }
    }

    /**
     * Проверка завершения построения слова
     *
     * @return Признак завершения построения слова
     */
    private boolean isWordComplete() {
        int childCount = targetLayout.getChildCount();
        for (int i = 0; i < childCount; i++) {
            TextView target = (TextView) targetLayout.getChildAt(i);
            if (target.getText().equals(CHARACTER_PLACEHOLDER)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Проверка результата
     *
     * @return Признак правильности составленного слова
     */
    private boolean checkResult() {
        if (!isWordComplete()) return false;

        boolean success = true;

        int childCount = targetLayout.getChildCount();
        for (int i = 0; i < childCount; i++) {
            TextView characterTextView = (TextView) targetLayout.getChildAt(i);
            int index = (Integer) characterTextView.getTag();
            Character character = characterTextView.getText().charAt(0);
            if (!game.checkCharacter(character, index)) {
                success = false;
                break;
            }
        }

        return success;
    }

    /**
     * Воспроизведение текущего слова
     */
    private void sayCurrentWord() {
        try {
            // Если воспроизведение не завершено, то звук не проигрывается
            if (mPlayer == null){
                mPlayer = new MediaPlayer();
                Uri soundUri = Uri.parse("android.resource://" + getApplicationContext().getPackageName()
                        + "/raw/" + removeExtension(game.getSound()));
                mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mPlayer.setDataSource(getApplicationContext(), soundUri);
                mPlayer.prepare();
                mPlayer.start();
                mPlayer.setOnCompletionListener(onCompletionListener);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    /**
     * Обработчик завершения воспроизведения
     */
    private MediaPlayer.OnCompletionListener onCompletionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            // Освобождаем ресурсы
            mPlayer.release();
            mPlayer = null;
        }
    };

    /**
     * Удаление расширения файла
     * @param fileName Имя файла
     * @return Имя файла без расширения
     */
    public static String removeExtension(String fileName) {

        String separator = System.getProperty("file.separator");
        String filename;

        // Remove the path upto the filename.
        int lastSeparatorIndex = fileName.lastIndexOf(separator);
        if (lastSeparatorIndex == -1) {
            filename = fileName;
        } else {
            filename = fileName.substring(lastSeparatorIndex + 1);
        }

        // Remove the extension.
        int extensionIndex = filename.lastIndexOf(".");
        if (extensionIndex == -1)
            return filename;

        return filename.substring(0, extensionIndex);
    }
}
