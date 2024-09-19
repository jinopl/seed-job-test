@Grab(group='org.yaml', module='snakeyaml', version='1.29')
import org.yaml.snakeyaml.Yaml

def yaml = new Yaml()
def jobfilelocation = '/var/lib/jenkins/workspace/seed-job/jobs.yaml'
def config = yaml.load(new File("${jobfilelocation}").text)


// Create folders first
def folders = config.jobs.collect { it.jobpath.split('/')[0] }.unique()
folders.each { folderName ->
    folder(folderName) {
        description("Folder for ${folderName} jobs")
    }
}

// Create all jobs inside the respective folders
config.jobs.each { job ->
    def (folderName, viewName) = job.jobpath.split('/')
    println "Job: ${job}"
    
    pipelineJob("${folderName}/${job.name}") {
        description(job.description)
        definition {
            cpsScm {
                scm {
                    git {
                        remote {
                            url(job.scm.git.url)
                            credentials(job.scm.git.token)
                        }
                        branch(job.scm.git.branch)
                    }
                }
                scriptPath(job.jfpath)
            }
        }
    }
}

// Create and populate views
def viewMap = [:]
config.jobs.each { job ->
    println "Job: ${job}"
    def (folderName, viewName) = job.jobpath.split('/')
    def viewKey = "${folderName}/${viewName}"
    
    // Initialize the list for this view if it's not already present
    if (!viewMap.containsKey(viewKey)) {
        viewMap[viewKey] = []
    }
    
    // Add job to the respective view based on the folder and view name
    viewMap[viewKey] << "${folderName}/${job.name}"
}
// Print viewMap for debugging
println "viewMap:"
viewMap.each { viewPath, jobNames ->
    println "viewPath: ${viewPath} -> jobNames: ${jobNames}"
