package ca.sheridancollege.pennyjobs.controllers;

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

import ca.sheridancollege.pennyjobs.beans.Account;
import ca.sheridancollege.pennyjobs.beans.Job;
import ca.sheridancollege.pennyjobs.beans.JobPoster;
import ca.sheridancollege.pennyjobs.beans.Student;
import ca.sheridancollege.pennyjobs.repositories.AccountRepository;
import ca.sheridancollege.pennyjobs.repositories.JobRepository;
import ca.sheridancollege.pennyjobs.repositories.StudentRepository;

@Controller
public class JobController {
	
	@Autowired
	private JobRepository jRepo;
	
	@Autowired
	private AccountRepository accountRepo;
	
	@Autowired
	private StudentRepository studentRepo;
	
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
	
	@GetMapping("/jobpost")
	public String loadAddJob(Model model, @ModelAttribute Job job) {
		model.addAttribute("job", new Job());
		return "JobForm.html";
	}
	
	@PostMapping("/jobpost")
	public String addJob(Model model, @ModelAttribute Job job, Authentication auth) {
		Account account = accountRepo.findByEmail(auth.getName());
		
		if (account.getPoster() != null) {
			JobPoster jobposter = account.getPoster();
			job.setJobPoster(jobposter);
			
			jRepo.save(job);
				
			//added if statement so program wont crash
			if (jobposter.getId() != null) {
				model.addAttribute("jobs", jRepo.findByJobPosterId(jobposter.getId()));
			}
		}
				
		model.addAttribute("job", new Job());
		
		return "JobForm.html";
	}

	@GetMapping("delete/{id}")
	public String deletePlayer(@PathVariable int id, Model model, Authentication auth) {
		Account account = accountRepo.findByEmail(auth.getName());
		JobPoster jobposter = account.getPoster();
		jRepo.deleteById(id);
		//added if statement so program wont crash
		if (jobposter.getId() != null) {
			model.addAttribute("jobs", jRepo.findByJobPosterId(jobposter.getId()));
		}
		return "poster.html";
	}
	
	
	@GetMapping("/joblist")
	public String loadJobList() {
		return "viewjobs.html";
	}
	
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
	
	@GetMapping("/jobs")
	public String leadSearch(Model model) {
		
		model.addAttribute("jobs", jRepo.findAll());
		
		return "jobs.html";
	}
	
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
		}
		model.addAttribute("jobs", results);
		
		return "jobs.html";
	}
	
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
					System.out.println("isStudent value changed: " + isStudent);
				}
				
			}
			model.addAttribute("isStudent", isStudent);
		} else {
			return "jobs.html";
		}
		return "jobdetails.html";
	}
	
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
}
