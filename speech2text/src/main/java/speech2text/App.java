package speech2text;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.apache.commons.io.FileUtils;

import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.speech.v1p1beta1.LongRunningRecognizeMetadata;
import com.google.cloud.speech.v1p1beta1.LongRunningRecognizeResponse;
import com.google.cloud.speech.v1p1beta1.RecognitionAudio;
import com.google.cloud.speech.v1p1beta1.RecognitionConfig;
import com.google.cloud.speech.v1p1beta1.RecognitionConfig.AudioEncoding;
import com.google.cloud.speech.v1p1beta1.SpeechClient;
import com.google.cloud.speech.v1p1beta1.SpeechContext;
import com.google.cloud.speech.v1p1beta1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1p1beta1.SpeechRecognitionResult;
import com.google.cloud.storage.Acl;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

/**
 * Hello world!
 *
 */
public class App {
	public static void main(String[] args) throws IOException, InterruptedException, ExecutionException {
		File dirPath = new File("C:\\Users\\vaibhav\\Documents\\vice");

		for (File file : dirPath.listFiles()) {
			// C:\Users\vaibhav\Documents\vice
			try {
				chaemli(file);
			} catch (Exception e) {
				e.printStackTrace();
				System.err.println(file.getName());
			}
			//break;
		}

		// pyar();
		// chaemli();
	}

	private static void pyar() throws IOException {
		File dirPath = new File("C:\\Users\\vaibhav\\Downloads\\CallLog_20181020-183336");

		for (File file : dirPath.listFiles()) {
			// C:\Users\vaibhav\Documents\vice
			String dest = "C:\\Users\\vaibhav\\Documents\\vice\\" + file.getName().replaceAll(" ", "");
			FileUtils.copyFile(file, new File(dest));

			String query = "C:\\Users\\vaibhav\\Downloads\\ffmpeg-20181018-f72b990-win64-static\\ffmpeg-20181018-f72b990-win64-static\\bin\\ffmpeg.exe -i "
					+ " " + dest + "  -codec:a pcm_mulaw -r 8000 " + " " + dest.replaceAll(".mp3", ".wav") + "";
			Process exec = new ProcessBuilder("CMD", "/C", query).start();

		}
	}

	private static void chaemli(File file) throws InterruptedException, ExecutionException, IOException {
		uploadFileGoogleStorage(file.getAbsolutePath(), file.getName());

		String gcsUri = "gs://istar-cloud-bucket/audio/" + file.getName();

		try (SpeechClient speech = SpeechClient.create()) {
			// Configure request with video media type
			SpeechContext ctx = SpeechContext.newBuilder().addPhrases("Talentify").build();

			RecognitionConfig recConfig = RecognitionConfig.newBuilder()// .setEncoding(AudioEncoding.UNRECOGNIZED)
					.setDiarizationSpeakerCount(2).setEnableSpeakerDiarization(true).setLanguageCode("en-US")
					.setSampleRateHertz(8000).setModel("phone_call").setUseEnhanced(true)
					.setEnableAutomaticPunctuation(true).addSpeechContexts(0, ctx).setMaxAlternatives(1)
					// .setAudioChannelCount(2).setEnableSeparateRecognitionPerChannel(true)
					.build();

			RecognitionAudio audio = RecognitionAudio.newBuilder().setUri(gcsUri).build();

			OperationFuture<LongRunningRecognizeResponse, LongRunningRecognizeMetadata> response = speech
					.longRunningRecognizeAsync(recConfig, audio);

			while (!response.isDone()) {
				System.out.println("Waiting for response...");
				Thread.sleep(10000);
			}
			// Just print the first result here.
			StringBuffer sbf = new StringBuffer();
			for (SpeechRecognitionResult result : response.get().getResultsList()) {

				// There can be several alternative transcripts for a given chunk of speech.
				// Just use the
				// first (most likely) one here.
				int i = 0;
				for (SpeechRecognitionAlternative alternative : result.getAlternativesList()) {
					// System.out.printf(i + "Transcript : %s\n", alternative.getTranscript());
					// System.out.printf(i+"Channel Tag : %s\n\n", result.getChannelTag());
					System.out.format("Speaker Tag %s: %s\n",
							alternative.getWords((alternative.getWordsCount() - 1)).getSpeakerTag(),
							alternative.getTranscript());
					sbf.append("Speaker Tag :" + alternative.getWords((alternative.getWordsCount() - 1)).getSpeakerTag()
							+ " " + alternative.getTranscript());
					i++;
				}
				BufferedWriter bwr = new BufferedWriter(
						new FileWriter(new File(file.getAbsolutePath().replaceAll(".wav", ".txt"))));

				// write contents of StringBuffer to a file
				bwr.write(sbf.toString());

				// flush the stream
				bwr.flush();

				// close the stream
				bwr.close();
				// SpeechRecognitionAlternative alternative =
				// result.getAlternativesList().get(0);

				// Print out the result

			}
		}
	}

	public static String uploadFileGoogleStorage(String filePath, String fileName) {
		Storage storage = StorageOptions.getDefaultInstance().getService();
		String bucketName = "istar-cloud-bucket";
		String folderName = "audio/";
		File file = new File(filePath);
		FileInputStream is = null;
		try {
			is = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			e.printStackTrace();

		}
		BlobId blobId = BlobId.of(bucketName, folderName + fileName);
		BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType("application/octet-stream").build();
		Blob blob = storage.create(blobInfo, is);
		Acl acl = storage.createAcl(blobId, Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER));
		return blob.getMediaLink();
	}
}