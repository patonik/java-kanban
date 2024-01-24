public class Main {
    public static void main(String[] args) {
        Manager manager = new Manager();
        try {
            manager.createTask(new Task("newTask", "description of the new Task", manager.generateId()));
            manager.createEpic(new Epic("newEpic", "description of the new Epic", manager.generateId()));
            manager.createSubtask(
                    new Subtask(
                            "newSubTask",
                            "description of the new SubTask",
                            manager.generateId(),
                            manager.getAllEpics().get(0)
                    )
            );
        } catch (Manager.IdGeneratorOverflow e) {
            throw new RuntimeException(e);
        }
        System.out.println(manager.getAllTasks());
        System.out.println(manager.getAllSubtasks());
        System.out.println(manager.getAllEpics());
        manager.getAllTasks().get(0).setStatus(Status.DONE);
        System.out.println(manager.getAllTasks().get(0).getStatus());
        manager.getAllEpics().get(0).setStatus(Status.DONE);
        System.out.println(manager.getAllEpics().get(0).getStatus());
        manager.getAllSubtasks().get(0).setStatus(Status.DONE);
        System.out.println(manager.getAllSubtasks().get(0).getStatus());
        System.out.println(manager.getAllEpics().get(0).getStatus());
        System.out.println(manager.getSubtasksOfEpic(manager.getAllEpics().get(0)).get(0));
    }
}
