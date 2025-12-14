package io.nexstudios.internal.nms.v1_21_11.dialog;

import io.nexstudios.nexus.bukkit.dialog.NexDialogBuilder;
import io.nexstudios.nexus.bukkit.dialog.NexDialogBuilderFactory;

public class PaperDialogBuilderFactory implements NexDialogBuilderFactory {


    @Override
    public NexDialogBuilder create() {
        return new PaperDialogBuilder();
    }


}
