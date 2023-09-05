package translateapp;

import com.google.cloud.translate.Translate;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Translation;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


public class TranslatorApp {

    private static final String API_KEY_GOOGLE = "key"; 
    private static final String API_KEY = "key";
    private static final String API_BASE_URL = "key";

    
    public static void main(String[] args) throws IOException {
    	
    	String sourceText;
        String translatedText;

        String csvFile = "kelimeler.csv";
        String line;
        String delimiter = ";";
        
        // csv dosyasini guncellemek icin gecici liste olusturma
        List<List<String>> updatedLines = new ArrayList<>();

        //dosyanin satir satir okunmasi
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            
        	while ((line = br.readLine()) != null) {
        		//kelimleri ; karakterine gore ayirma ve listeye alma
                String[] words = line.split(delimiter);
                List<String> kelimeler = new ArrayList<>(Arrays.asList(words));
                
                
                //ilk kelimenin ingilizceye cevirilmesi ve es anlamlilarinin bulunmasi
                String firstTranslatedWord = translateText(words[0], "tr", "en");
                List<String> synonyms = getSynonyms(firstTranslatedWord);
                synonyms.add(firstTranslatedWord);
                
                //kelimeler listesinin icinden es anlamli olmayanlari cikarma
                for (int i = 0; i < kelimeler.size(); i++) {
                    sourceText = kelimeler.get(i);
                    translatedText = translateText(sourceText, "tr", "en");
                    boolean isSynonym = false;

                    for (String synonym : synonyms) {
                        
                    	if (synonym.equalsIgnoreCase(translatedText)) {
                            isSynonym = true;
                            break;
                        }
                    
                    }

                    if (!isSynonym) 
                    {
                        kelimeler.set(i, ""); //es anlamli olmayan kelimlerin silinmesi
                    }
                }
                //ilk kelimenin cevirisini listenin satir sonuna ekleme
                kelimeler.add(firstTranslatedWord);
                updatedLines.add(kelimeler);
            }

            // listeyi csv dosyasina yazma
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(csvFile))) {
                for (List<String> updatedLine : updatedLines) {
                    bw.write(String.join(delimiter, updatedLine));
                    bw.newLine();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


        
       
    
    //google translate apisini kullanarak kaynak dilden hedef dile cevirir donusturur ve string dondurur
    public static String translateText(String sourceText,String sourceLanguage, String targetLanguage) {
        try {
            Translate translate = TranslateOptions.newBuilder().setApiKey(API_KEY_GOOGLE).build().getService();
            Translation translation = translate.translate(sourceText, Translate.TranslateOption.sourceLanguage(sourceLanguage), Translate.TranslateOption.targetLanguage(targetLanguage));
            String translatedText = translation.getTranslatedText();
            return translatedText;
        } catch (Exception e) {
            e.printStackTrace();
            return "Error during translation: " + e.getMessage();
        }
    }
    
    //word parametresinin es anlamlilarini bir liste olarak dondurur
    public static List<String> getSynonyms(String word) {
        List<String> synonyms = new ArrayList<>();

        try {
            String apiUrl = API_BASE_URL + word;

            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("accept", "application/json");
            connection.setRequestProperty("X-Api-Key", API_KEY);

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                InputStream responseStream = connection.getInputStream();
                String responseContent = new Scanner(responseStream, "UTF-8").useDelimiter("\\A").next();
                responseStream.close();

                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(responseContent);

                JsonNode synonymsNode = root.path("synonyms");
                if (synonymsNode.isArray()) {
                    for (JsonNode synonymNode : synonymsNode) {
                        synonyms.add(synonymNode.asText());
                    }
                }
            } else {
                System.out.println("Error response code: " + connection.getResponseCode());
            }

            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return synonyms;
    }
}
