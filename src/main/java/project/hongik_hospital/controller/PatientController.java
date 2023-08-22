package project.hongik_hospital.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import project.hongik_hospital.domain.AccountType;
import project.hongik_hospital.domain.GenderType;
import project.hongik_hospital.domain.Patient;
import project.hongik_hospital.form.PatientForm;
import project.hongik_hospital.repository.PatientRepository;
import project.hongik_hospital.service.PatientService;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.Optional;

import static java.lang.Integer.*;
import static java.lang.Integer.valueOf;

@Controller
@Slf4j
@RequiredArgsConstructor
public class PatientController {

    private final PatientRepository patientRepository;
    private final PatientService patientService;

    @GetMapping("/patient/new")
    public String createForm(Model model) {

        model.addAttribute("form", new PatientForm());
        log.info("sign up");

        return "patient/createPatientForm";
    }

    @PostMapping("/patient/new")
    public String create(@Valid PatientForm form, Model model, BindingResult result) {
        if (result.hasErrors()) {
            return "patient/createPatientForm";
        }

        // 이미 가입된 아이딘지 확인하는 로직
        Optional<Patient> optionalPatient = patientService.findPatientByLoginId(form.getLogin_id());
        if (optionalPatient.isPresent()) {
            result.rejectValue("login_id", "invalid", "Login id already exists");
            model.addAttribute("form", new PatientForm());
            model.addAttribute("isIdAlreadyExist", true);
            return "patient/createPatientForm";
        }

        Patient patient = new Patient(form.getName(), parseInt(form.getAge()), form.getGender());
        patient.setLogIn(form.getLogin_id(), form.getLogin_pw());

        patientService.join(patient);
        return "redirect:/";
    }

    @GetMapping("/patient/login")
    public String signIn(Model model) {
        model.addAttribute("form", new PatientForm());
        log.info("Login page");
        return "loginForm";
    }

    @PostMapping("/patient/login")
    public String beforeSignIn(@Valid PatientForm form, BindingResult result, HttpSession session, Model model) {
        // Check if the login credentials are valid
        Optional<Patient> findPatient = patientService.findPatient(form.getLogin_id(), form.getLogin_pw());

        if (findPatient.isEmpty()) {  // 등록된 회원이 없는 경우
            result.rejectValue("login_id", "invalid", "Invalid login credentials");
            model.addAttribute("form", new PatientForm());
            model.addAttribute("isSignInFail", true); // 등록된 회원이 아닌 경우 경고 발생
            return "loginForm";
        }

        Patient patient = findPatient.get();
        // Set the 'loggedIn' attribute to true and store the patient's information
        model.addAttribute("patient", patient);
        model.addAttribute("loggedIn", true);

        // 사용자 정보를 세션에 저장
        session.setAttribute("loggedInUser", patient);

        if (patient.getAccountType() == AccountType.ADMIN) {
            // 운영자 전용 페이지로 이동
            return "admin/home";
        }

        log.info("Logged in: " + patient.getName());
        return "redirect:/"; // Redirect to the home page
    }

    @GetMapping("/patient/logout")
    public String patientLogout(HttpSession session, Model model) {
        // Clear the session or do necessary logout operations

        model.addAttribute("loggedIn", false);

        // 사용자 정보를 세션에서 제거
        session.removeAttribute("loggedInUser");

        log.info("Logged out");
        return "redirect:/"; // Redirect to the home page
    }

    @GetMapping("/patient")
    public String patientInfo(HttpSession session, Model model) {
        // 로그인한 회원 조회
        Patient loggedInUser = (Patient) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            model.addAttribute("form", new PatientForm());
            model.addAttribute("needToSignIn", true);
            return "loginForm";
        }

        // 로그인한 회원의 id로 회원 엔티티 조회(그냥 loggedInUser를 view에 넘기게 된다면 해당 엔티티가 수정된 경우에도 화면에는 수정전 데이터가 표시됨)
        // 즉 더티체킹의 영향을 받지 않는 똑같은 객체만 사용할 수 밖에 없어서 사용하려는 id는 동일하므로 db에서 새롭게 조회한다.
        Patient patient = patientService.findPatient(loggedInUser.getId());
        model.addAttribute("patient", patient);

        log.info("User info: " + loggedInUser.getLogIn().getLogin_id());
        return "patient/information";
    }

    @GetMapping("/patient/{patientId}/edit")
    // 보안적인 측면에서 본다면 {patientId}를 외부로 노출하지 않고 HttpSession 에서 getId 해서 회원 조회하는 것이 보안적인 측면에서 좋을듯?
    public String changePatientInfo(@PathVariable Long patientId, Model model) {
        // 로그인한 회원 조회
        Patient loggedInUser = patientService.findPatient(patientId);
        model.addAttribute("patient", loggedInUser);

        PatientForm patientForm = new PatientForm();
        patientForm.setPatientId(patientId);
        model.addAttribute("form", patientForm);

        log.info("Change info: " + loggedInUser.getLogIn().getLogin_id());
        return "patient/editInformation";
    }

    @PostMapping("/patient/{patientId}/edit")
    public String updatePatientInfo(@PathVariable Long patientId, @Valid PatientForm form) {

        // 로그인한 회원 조회
        Patient loggedInUser = patientService.findPatient(patientId);
        System.out.println("loggedInUser = " + loggedInUser);

        // form으로 새롭게 가져온 데이터
        String loginId = loggedInUser.getLogIn().getLogin_id();  //id는 변경하지 않음
        String loginPw = form.getLogin_pw();
        String name = form.getName();
        int age = parseInt(form.getAge());
        GenderType gender = form.getGender();

        patientService.update(patientId, loginId, loginPw, name, age, gender);

        return "redirect:/";
    }

    @GetMapping("/patient/delete")
    public String deletePatient(HttpSession session, Model model) {
        Patient loggedInUser = (Patient) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            model.addAttribute("form", new PatientForm());
            model.addAttribute("needToSignIn", true);
            return "loginForm";
        }

        return "patient/deletePatient";
    }

    @PostMapping("/patient/delete")
    public String delete(HttpSession session, Model model) {
        Patient loggedInUser = (Patient) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/";
        }

        Patient patient = patientService.findPatient(loggedInUser.getId());
        if (patient != null) {
            // 여기서 데이터베이스에서 사용자 정보를 삭제
            if (patientService.removePatient(patient.getId())) { // 이 부분은 removePatient 메서드의 반환값에 따라 수정되어야 할 수 있습니다.
                // 사용자 정보를 세션에서 제거
                session.removeAttribute("loggedInUser");
            }
        }

        return "redirect:/";
    }
}

