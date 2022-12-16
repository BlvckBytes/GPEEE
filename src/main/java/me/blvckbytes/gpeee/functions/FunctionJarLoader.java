/*
 * MIT License
 *
 * Copyright (c) 2022 BlvckBytes
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package me.blvckbytes.gpeee.functions;

import me.blvckbytes.gpeee.functions.std.AStandardFunction;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class FunctionJarLoader {

  /**
   * Tries to load a standard function from a JAR file
   * @param file JAR file to load from
   * @return Instance of the loaded function if present, null otherwise
   * @throws Exception Invalid file or unloadable class
   */
  public @Nullable AStandardFunction loadFunctionFromFile(File file) throws Exception {
    Class<?> target = loadExternalClass(file, AStandardFunction.class);

    if (target == null)
      return null;

    return (AStandardFunction) target.getDeclaredConstructor().newInstance();
  }

  /**
   * Try to load a class which has been linked against inside an external JAR file
   * @param file JAR file to load from
   * @param target Target class which has been linked against
   * @return The loaded class if present, null otherwise
   * @param <T> Superclass type of the target class
   * @throws IOException Could not load the file's URL
   * @throws ClassNotFoundException Could not load a class of the JAR by it's name
   */
  private<T> @Nullable Class<? extends T> loadExternalClass(File file, Class<T> target) throws IOException, ClassNotFoundException {

    if (!file.exists())
      return null;

    URL jar = file.toURI().toURL();

    // Create a new class-loader on the jar's path with the
    // class (within current context) as a parent
    URLClassLoader loader = new URLClassLoader(new URL[]{ jar }, target.getClassLoader());

    try (JarInputStream stream = new JarInputStream(jar.openStream())) {

      JarEntry entry;
      while ((entry = stream.getNextJarEntry()) != null) {
        String name = entry.getName();

        // Not a compiled java class file
        if (!name.endsWith(".class"))
          continue;

        // Transform path style FQN into package style
        name = name.substring(0, name.lastIndexOf('.')).replace('/', '.');

        // Instruct the class loader targeted at the JAR to load this FQN
        Class<?> loaded = loader.loadClass(name);

        // Check if the target class is actually assignable from this just loaded class
        if (!target.isAssignableFrom(loaded))
          continue;

        // Target class found, close the loader and stop the whole process
        // by returning out of the loop and all resource managed try-blocks

        loader.close();
        return loaded.asSubclass(target);
      }
    }

    // The jar didn't contain any usable class file
    return null;
  }
}
