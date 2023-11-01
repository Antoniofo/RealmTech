package ch.realmtechServer.cli;

import java.util.concurrent.Callable;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.ParentCommand;

@Command(name = "stop", description = "stop le serveur", mixinStandardHelpOptions = true)
public class StopCommand implements Callable<Integer> {
    @ParentCommand
    private MasterServerCommand masterServerCommand;
    @Override
    public Integer call() throws Exception {
        masterServerCommand.serverContext.close().await();
        return 0;
    }
}
