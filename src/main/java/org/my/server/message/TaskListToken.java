package org.my.server.message;

import com.google.gson.reflect.TypeToken;
import org.my.task.Task;

import java.util.List;

final class TaskListToken<T extends Task> extends TypeToken<List<T>> {

}
