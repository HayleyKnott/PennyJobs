package ca.sheridancollege.pennyjobs.controllers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import antlr.StringUtils;
import ca.sheridancollege.pennyjobs.beans.Account;
import ca.sheridancollege.pennyjobs.beans.Job;
import ca.sheridancollege.pennyjobs.beans.JobPoster;
import ca.sheridancollege.pennyjobs.beans.Parent;
import ca.sheridancollege.pennyjobs.beans.Student;
import ca.sheridancollege.pennyjobs.repositories.AccountRepository;
import ca.sheridancollege.pennyjobs.repositories.JobPosterRepository;
import ca.sheridancollege.pennyjobs.repositories.JobRepository;
import ca.sheridancollege.pennyjobs.repositories.ParentRepository;
import ca.sheridancollege.pennyjobs.repositories.StudentRepository;

/**
 * This class is used to handle the functionalities of jobs
 * @author Weiye Chen, Gregory Knott, Patrick Ferdinand Adhitama, Dimitrios Vlachos
 *
 */
@Controller
public class JobController {
	
	@Autowired
	private JobRepository jRepo;
	
	@Autowired
	private AccountRepository accountRepo;
	
	@Autowired
	private StudentRepository studentRepo;
	
	@Autowired
	private JobPosterRepository posterRepo;
	
	@Autowired
	private ParentRepository parentRepo;
	
	/**
	 * Redirect to the home page
	 * @param auth
	 * @return
	 */
	@GetMapping("/")
	public String loadRoot(Authentication auth) {
		
		//this will direct the user to the right homepage if they go back to root
		if (auth != null) {
			if (auth.isAuthenticated()) {
				return "redirect:/accountredirectpage";
			} else {
				return "WelcomePage.html";
			}
		}
		
		return "WelcomePage.html";
	}
	
	/**
	 * Load the job post page 
	 * @param model
	 * @param job
	 * @return
	 */
	@GetMapping("/jobpost")
	public String loadAddJob(Model model, @ModelAttribute Job job) {
		model.addAttribute("job", new Job());
		return "JobForm.html";
	}
	
	/**
	 * In the job post page to post a new job
	 * @param model
	 * @param job
	 * @param auth
	 * @return
	 */
	
	@PostMapping("/jobpost")
	public String addJob(Model model, @ModelAttribute Job job, Authentication auth) {
		Account account = accountRepo.findByEmail(auth.getName());
		
		if (account.getPoster() != null) {
			JobPoster jobposter = account.getPoster();
			job.setJobPoster(jobposter);
			job.setProofSubmitted(false);
			job.setStudentPaid(false);
			jRepo.save(job);
				
			//added if statement so program wont crash
			if (jobposter.getId() != null) {
				model.addAttribute("jobs", jRepo.findByJobPosterId(jobposter.getId()));
			}
		}
				
		model.addAttribute("job", new Job());
		
		return "JobForm.html";
	}
	
	/**
	 * Allow poster to delete their jobs
	 * @param id
	 * @param model
	 * @param auth
	 * @return
	 */
	@GetMapping("/delete/{id}")
	public String deletePosting(@PathVariable int id, Model model, Authentication auth) {
		Account account = accountRepo.findByEmail(auth.getName());
		JobPoster jobposter = account.getPoster();
		jRepo.deleteById(id);
		//added if statement so program wont crash
		if (jobposter.getId() != null) {
			model.addAttribute("jobs", jRepo.findByJobPosterId(jobposter.getId()));
		}
		return "redirect:/poster";
	}
	
	/**
	 * This method is allowed student and stundents'parent to view the jobs applied
	 * @param model
	 * @param auth
	 * @return
	 */
	@GetMapping("/viewjobs")
	public String loadJobList(Model model, Authentication auth) {
		Account account = accountRepo.findByEmail(auth.getName());
		
		//added if statement so program wont crash
		if (account.getStudent() != null) {
			Student student = account.getStudent();
			
			if (student.getId() != null) {
				model.addAttribute("jobs", jRepo.findByStudentId(student.getId()));
			}
		}
		
		return "ViewMyJobs.html";
	}
	
	
	/**
	 * Home page for the poster's page
	 * @param model
	 * @param auth
	 * @return
	 */
	@GetMapping("/poster")
	public String loadPoster(Model model, Authentication auth){
		
		Account account = accountRepo.findByEmail(auth.getName());
		
		model.addAttribute("name", account.getFirstName());
		
		//added if statement so program wont crash
		if (account.getPoster() != null) {
			
			JobPoster jobposter = account.getPoster();
			
			if (jobposter.getId() != null) {
				model.addAttribute("jobs", jRepo.findByJobPosterId(jobposter.getId()));
			}
		}
		
		return "poster.html";
	}
	
	/**
	 * View all jobs
	 * @param model
	 * @return
	 */
	@GetMapping("/jobs")
	public String leadSearch(Model model) {
		
		model.addAttribute("jobs", jRepo.findAll());
		
		return "jobs.html";
	}
	
	/**
	 * Search jobs in the same page
	 * @param model
	 * @param searchType
	 * @param query
	 * @return
	 */
	@GetMapping("/jobs/search")
	public String searchDB(Model model, @RequestParam String searchType, @RequestParam String query) {
		
		List<Job> results = new ArrayList<Job>();
		
		switch (searchType){
		case "Title":
			results = jRepo.findAllByTitleIgnoreCaseContains(query);
			break;
		case "Description":
			results = jRepo.findAllByDescriptionIgnoreCaseContains(query);
			break;
		case "City":
			List<Job> allJobs = (List<Job>) jRepo.findAll();
			for (Job job : allJobs) {
				String city = job.getAddress().getCity().toUpperCase();
				if (city.contains(query.toUpperCase())) {
					results.add(job);
				}
			}
			//If no query was searched, return all jobs
			if (query.isEmpty()) {
				results = allJobs;
			}
		}
		model.addAttribute("jobs", results);
		
		return "jobs.html";
	}
	
	/**
	 * View jobs' details
	 * @param id
	 * @param model
	 * @param auth
	 * @return
	 */
	@GetMapping("/jobs/{id}")
	public String viewJob(@PathVariable int id, Model model, Authentication auth) {
		boolean isStudent = false;
		if (!jRepo.findById(id).isEmpty()) {
			Job job = jRepo.findById(id).get();
			model.addAttribute("job", job);
			//if the user viewing the jobs is a student pass that along so they can see the apply for job button
			if (auth.isAuthenticated()) {
				Account account = accountRepo.findByEmail(auth.getName());
				if (account.getAccountType().equals("S")) {
					isStudent = true;
					Student student = studentRepo.findByAccount(account);
					model.addAttribute("student", student);
				}
				else if (account.getAccountType().equals("J")) {
					JobPoster poster = posterRepo.findByAccount(account);
					model.addAttribute("jobposter", poster);
				}
				else if (account.getAccountType().equals("P")) {
					Parent parent = parentRepo.findByAccount(account);
					model.addAttribute("parent", parent);
				}
			}
			model.addAttribute("isStudent", isStudent);
		} else {
			return "jobs.html";
		}
		return "jobdetails.html";
	}
	
	/**
	 * Assign the job to student 
	 * @param id
	 * @param model
	 * @param auth
	 * @return
	 */
	@GetMapping("/assign/{id}")
	public String assignJob(@PathVariable int id, Model model, Authentication auth) {
		
		if (!jRepo.findById(id).isEmpty()) {
			Job job = jRepo.findById(id).get();
			
			if (auth.isAuthenticated()) {
				Account account = accountRepo.findByEmail(auth.getName());
				Student student = studentRepo.findByAccount(account);
				job.setStudent(student);
				jRepo.save(job);
			}
		}
		
		return "redirect:/jobs";
	}
	
	@GetMapping("/parent/studentjobs")
	public String loadStudentJobs(Model model, Authentication auth) {
		
		if (auth.isAuthenticated()) {
			Account account = accountRepo.findByEmail(auth.getName());
			if (account.getAccountType().equals("P")) {
				Parent parent = parentRepo.findByAccount(account);
				
				if (parent.getStudent() != null) {
				
					List studentJobs = jRepo.findByStudentId(parent.getStudent().getId());
					
					if (!studentJobs.isEmpty()) {
						model.addAttribute("jobs", studentJobs);
					}
				}
			}
		}
		
		return "childjobs.html";
	}
	
	@GetMapping("/jobs/{id}/proof")
	public String loadSubmitProof(@PathVariable int id, Model model, Authentication auth) {
		
		boolean isStudent = false;
		if (!jRepo.findById(id).isEmpty()) {
			Job job = jRepo.findById(id).get();
			model.addAttribute("job", job);
			//if the user viewing the jobs is a student pass that along so they can see the apply for job button
			if (auth.isAuthenticated()) {
				Account account = accountRepo.findByEmail(auth.getName());
				if (account.getAccountType().equals("S")) {
					isStudent = true;
				}
			}
			model.addAttribute("isStudent", isStudent);
		}
		
		return "submitproof.html";
	}
	
	@PostMapping("/uploadproof")
	public String uploadProof(@RequestParam("photo") MultipartFile imageProof, @RequestParam("jobId") int inputJobId, Model model) throws IOException {
		
		Job job = jRepo.findById(inputJobId).get();
		
		String directory = "webapps/ROOT/completed-job-photos/" + job.getId();
		
		saveImage(directory, "completed-job-" + job.getId() +".jpg", imageProof);
		
		job.setProofSubmitted(true);
		jRepo.save(job);
		
		return "redirect:/jobs/" + inputJobId;
	}
	
	@PostMapping("/paystudent")
	public String payStudent(Model model, @RequestParam("jobId") int jobId, Authentication auth) throws IOException {
		
		Job job = jRepo.findById(jobId).get();
		model.addAttribute("job",job);
		
		if (auth.isAuthenticated()) {
			Account account = accountRepo.findByEmail(auth.getName());
			if (account.getAccountType().equals("J")) {
				model.addAttribute("account", account);
				
				Student student = job.getStudent();
				model.addAttribute("student", student);
			}
		}
		
		return "paystudent.html";
	}
	
	@PostMapping("/review")
	public String reviewStudent(@RequestParam("jobId") int jobId, 
			@RequestParam("rating") int rating, @RequestParam("paid") boolean paid){
		
		Job job = jRepo.findById(jobId).get();
		Student student = job.getStudent();
		
		if (paid) {
			job.setStudentPaid(true);
			jRepo.save(job);
		}
		
		//Calculate and set new overall student rating
		
		//Get list of all jobs completed by student
		List<Job> studentJobs = jRepo.findByStudentId(student.getId());
		int completedJobs = 0;
		
		//Get number of completed jobs 
		for (Job j : studentJobs) {
			if (j.getStudentPaid() == true) {
				completedJobs++;
			}
		}
		double ratingAverage = (rating - student.getRating()) / completedJobs;
		double newRating = student.getRating() + ratingAverage;
		
		student.setRating(newRating);
		studentRepo.save(student);
		
		return "redirect:/jobs/" + jobId;
	}
	
	public void saveImage(String directory, String fileName,
            MultipartFile image) throws IOException {
		
        Path path = Paths.get(directory);
         
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
         
        try (InputStream stream = image.getInputStream()) {
            Path resolvedPath = path.resolve(fileName);
            Files.copy(stream, resolvedPath, StandardCopyOption.REPLACE_EXISTING);

        } catch (IOException ex) {        
            throw new IOException("Could not save image, Exception: " + ex);
        }      
    }

}
