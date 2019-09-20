package az.blacklist.person.verification.scheduler;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;

import org.apache.logging.log4j.core.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.stereotype.Component;

import az.blacklist.person.verification.service.black.list.BlackListService;
import az.blacklist.person.verification.service.world.check.WorldCheckService;

@EnableScheduling
@Component
public class DocumentsScheduler {

	private static final Logger logger = LoggerFactory.getLogger(DocumentsScheduler.class);

	private final BlackListService blackListService;
	private final WorldCheckService worldCheckService;
	private final String uploadPath;
	private final String archivePath;

	public DocumentsScheduler(BlackListService blackListService, WorldCheckService worldCheckService,
			@Value("${file.upload.path}") String uploadPath, @Value("${file.archive.path}") String archivePath) {
		this.blackListService = blackListService;
		this.worldCheckService = worldCheckService;
		this.uploadPath = uploadPath;
		this.archivePath = archivePath;
	}

	@Bean
	public TaskScheduler taskScheduler() {
		return new ConcurrentTaskScheduler();
	}

	@Scheduled(fixedDelay = 10_000)
	public void persistFile() {

		logger.debug("schedule start");

		File folder = new File(uploadPath);
		File[] listOfFiles = folder.listFiles();

		if (listOfFiles == null)
			return;

		Arrays.stream(listOfFiles).filter(this::validForProcess).forEach(file -> {
			if (FileUtils.getFileExtension(file).equals("xls") || FileUtils.getFileExtension(file).equals("xlsx")) {
				logger.info("Black List file proceed {}", file.getName());
				blackListService.readBlackList(file);
			} else {
				logger.info("World Check file proceed {}", file.getName());
				if (file.getName().contains("delete")) {
					worldCheckService.processDeleteFile(file);
				} else {
					worldCheckService.processUpdateFile(file);
				}
			}
			try {
				archiveFile(file);
			} catch (IOException e) {
				logger.error("Error while archive documents", e);
			}
		});

		logger.debug("schedule end");
	}

	private boolean validForProcess(File file) {
		return file.isFile() && (file.getName().endsWith(".xls") || file.getName().endsWith(".xlsx")
				|| file.getName().endsWith(".xml"));
	}

	private void archiveFile(File file) throws IOException {

        String fileExtension = FileUtils.getFileExtension(file);
        String fileName = String.format("%s-%s.%s",
                file.getName().replace(String.format(".%s", fileExtension), ""),
                LocalDateTime.now(),
                fileExtension);

        Files.move(file.toPath(), Paths.get(String.format("%s/%s",
                archivePath,
                fileName)),
                REPLACE_EXISTING
        );
    }
}