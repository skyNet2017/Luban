package top.zibin.luban;

import java.io.File;

public class NoResizeRenameListener implements OnRenameListener {
    @Override
    public String rename(String filePath) {
        try {
            return filePath.substring(filePath.lastIndexOf("/") + 1, filePath.lastIndexOf("."))
                    + Luban.FILE_PREFIX_NO_RESIZE + filePath.substring(filePath.lastIndexOf("."));
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            return new File(filePath).getName();
        }

    }
}
