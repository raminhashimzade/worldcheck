package az.blacklist.person.verification.scheduler;

import az.blacklist.person.verification.service.black.list.BlackListService;
import az.blacklist.person.verification.service.world.check.WorldCheckService;
import org.apache.logging.log4j.core.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.LocalDateTime;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;

@Component
public class DocumentsScheduler {

    private static final Logger logger = LoggerFactory.getLogger(DocumentsScheduler.class);

    private final BlackListService blackListService;
    private final WorldCheckService worldCheckService;
    private final String uploadPath;
    private final String archivePath;


    public DocumentsScheduler(BlackListService blackListService,
                              WorldCheckService worldCheckService,
                              @Value("${file.upload.path}") String uploadPath,
                              @Value("${file.archive.path}") String archivePath) {
        this.blackListService = blackListService;
        this.worldCheckService = worldCheckService;
        this.uploadPath = uploadPath;
        this.archivePath = archivePath;
    }

    @Scheduled(fixedDelay = Long.MAX_VALUE)
    public void persistFile() {

        try {
            WatchService watchService = FileSystems.getDefault().newWatchService();

            Path logDir = Paths.get(uploadPath);
            logDir.register(watchService, ENTRY_CREATE);

            WatchKey key;
            while ((key = watchService.take()) != null) {
                try {
                    for (WatchEvent<?> event : key.pollEvents()) {
                        Path dir = (Path) key.watchable();

                        File file = new File(String.format("%s/%s", dir, event.context().toString()));

                        if (FileUtils.getFileExtension(file).equals("xls") ||
                                FileUtils.getFileExtension(file).equals("xlsx")) {
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

                        archiveFile(file);
                    }

                    key.reset();
                } catch (Exception e) {
                    logger.error("Error while listening documents", e);
                }
            }
        } catch (IOException | InterruptedException e) {
            logger.error("Error while listening documents", e);
        }
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
