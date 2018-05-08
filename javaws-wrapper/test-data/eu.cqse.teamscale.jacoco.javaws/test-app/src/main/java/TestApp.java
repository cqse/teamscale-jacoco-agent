import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestApp {
	
	public static void main(String[] args) {
		System.out.println(String.join("\n", args));
		System.err.println("error");
		
		String arguments = String.join(" ", args);
		Pattern pattern = Pattern.compile("exitCode=(\\d+)", Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(arguments);
		
		int exitCode = 0;
		if (matcher.find()) {
			String exitCodeString = matcher.group(1);
			exitCode = Integer.valueOf(exitCodeString);
		}
		System.exit(exitCode);
	}

}
