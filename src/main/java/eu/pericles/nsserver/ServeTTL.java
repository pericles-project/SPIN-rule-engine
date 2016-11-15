package eu.pericles.nsserver;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.file.Files;

/**
 * Created by fabio on 15/11/2016.
 */
public class ServeTTL extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {

        File base = new File("/home/fcorubolo/namespace");

        String filename = URLDecoder.decode(request.getPathInfo().substring(1), "UTF-8");
        File file = new File(base, filename);
        if (!file.getParentFile().equals(base)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        response.setHeader("Content-Type", "text/turtle");
        response.setHeader("Content-Length", String.valueOf(file.length()));
        //response.setHeader("Content-Disposition", "inline; filename=\"" + file.getName() + "\"");
        Files.copy(file.toPath(), response.getOutputStream());
    }


}
