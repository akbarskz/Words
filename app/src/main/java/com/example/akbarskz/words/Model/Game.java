package com.example.akbarskz.words.Model;

import com.example.akbarskz.words.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

public class Game {
    private Theme[] themes;
    private Word currentWord;
    private Map<Character, List<Integer>> charPositions;

    public Game(Theme[] themes) {
        charPositions = new TreeMap<>();
        this.themes = themes;
    }

    /**
     * Инициализация случайного слова
     */
    public Pair<Theme, Word> nextWord() {
        Random rnd = new Random();
        Theme theme = themes[rnd.nextInt(themes.length)];
        currentWord = theme.getWords().get(rnd.nextInt(theme.getWords().size()));
        fillCharPositions();
        return new Pair(theme, currentWord);
    }

    /**
     * Разбивка слова на буквы
     */
    private void fillCharPositions() {
        charPositions.clear();

        // Разбиваем слово на символы и заполняем charPositions
        for (int i = 0; i < currentWord.getWordEn().length(); i++) {
            Character character = Character.toLowerCase(currentWord.getWordEn().charAt(i));
            if (!charPositions.containsKey(character)) {
                charPositions.put(character, new ArrayList<Integer>());
            }
            charPositions.get(character).add(i);
        }
    }

    /**
     * Формирование списка букв слова располженных в произвольном порядке
     *
     * @return Список букв
     */
    public ArrayList<Character> getRandomCharacters() {
        ArrayList<Character> characters = new ArrayList<>();
        for (Character character : charPositions.keySet()) {
            int characterCount = charPositions.get(character).size();
            for (int i = 0; i < characterCount; i++) {
                characters.add(character);
            }
        }

        Random rnd = new Random();
        int characterCount = characters.size();
        for (int i = 0; i < characterCount; i++) {
            int newPosition = rnd.nextInt(characterCount);
            char character = characters.get(i);
            characters.set(i, characters.get(newPosition));
            characters.set(newPosition, character);
        }

        return characters;
    }

    /**
     * Проверка нахождения буквы в указанной позиции
     * @param character
     * @param position
     * @return
     */
    public boolean checkCharacter(Character character, Integer position) {
        if (!charPositions.containsKey(character)) {
            throw new RuntimeException("Character '" + character + "' not found");
        }
        return charPositions.get(character).contains(position);
    }

    /**
     * MP3 файл для текущего слова
     * @return
     */
    public String getSound() {
        return currentWord.getSound();
    }
}
