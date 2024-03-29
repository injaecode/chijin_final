package stu.member.login;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Random;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import stu.common.common.CommandMap;
import stu.member.join.JoinService;

@Controller
public class LoginController {

	Logger log = Logger.getLogger(this.getClass());

	@Resource(name = "loginService")
	private LoginService loginService;

	@Resource(name = "joinService")
	private JoinService joinService;


	@RequestMapping(value = "/loginForm.do")
	public ModelAndView loginForm(CommandMap commandMap) throws Exception {
		ModelAndView mv = new ModelAndView("login/loginForm");

		return mv;
	}

	// 로그인 이후 메인페이지 이동
	@RequestMapping(value = "/loginAction.do", method = RequestMethod.POST)
	public ModelAndView loginAction(CommandMap commandMap, HttpServletRequest request) throws Exception {
		ModelAndView mv = new ModelAndView();
		HttpSession session = request.getSession();

		Map<String, Object> chk = loginService.loginAction(commandMap.getMap());
		Map<String, Object> fnd = loginService.findID(commandMap.getMap());
		
		//case1. id 없음 -> id/비밀번호 불일치 메시지 보내기
		if (fnd==null) {
			log.info("************************case1 id 없음");
			mv.setViewName("login/loginForm");
			mv.addObject("message", "해당 아이디 혹은 비밀번호가 일치하지 않습니다.");
		}
		
		//case2. id 존재 
		else {
			BigDecimal loginCountBD = (BigDecimal) fnd.get("MEMBER_LOGIN_COUNT");
			int loginCount = loginCountBD.intValue();
			//case 2.1 삭제된 id
			if(fnd.get("MEMBER_DELETE").equals("1")) {
				mv.setViewName("login/loginForm");
				mv.addObject("message", "탈퇴한 회원 입니다.");		
			}
			//case 2.2 로그인 시도 횟수 초과
			else if(loginCount > 4) {
				log.info("***********************************************************카운트 체크");
				mv.setViewName("login/loginForm");
				mv.addObject("message", "일일 로그인 시도 횟수(5회)를 초과하셨습니다.");
			}
			
			//case 2.3. id, pw 틀려서 로그인 불가
			else if(chk==null) {
				mv.setViewName("login/loginForm");
				mv.addObject("message", "해당 아이디 혹은 비밀번호가 일치하지 않습니다.");
				
				//일치하는 id가 있을 경우 MEMBER_LOGIN_COUNT값 증가시키기
				loginService.loginCountUpdate(commandMap.getMap());
			}
			
			//case 2.4 로그인 성공
			else {
				session.setAttribute("SESSION_ID", chk.get("MEMBER_ID"));
				session.setAttribute("SESSION_NO", chk.get("MEMBER_NO"));
				session.setAttribute("SESSION_NAME", chk.get("MEMBER_NAME"));

				mv = new ModelAndView("redirect:/main.do");
				mv.addObject("MEMBER", chk);

				session.getMaxInactiveInterval();
			}
		}
		
		return mv;
	}

	// 소셜로그인 이후 메인페이지 이동
	@RequestMapping(value = "/socialLoginAction.do", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> googleLoginAction(@RequestBody Map<String, Object> map, HttpServletRequest request)
			throws Exception {

		HttpSession session = request.getSession();

		session.setAttribute("SESSION_ID", map.get("ID"));
		session.setAttribute("SESSION_NO", map.get("ID"));
		session.setAttribute("SESSION_NAME", map.get("Name"));

		session.getMaxInactiveInterval();

		String url = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort()
				+ request.getContextPath() + "/main.do";
		map.put("URL", url);

		return map;
	}

	// 네이버 로그인 Callback 페이지
	@RequestMapping(value = "/loginCallback.do")
	public ModelAndView loginCallback(CommandMap commandMap) throws Exception {
		ModelAndView mv = new ModelAndView("/loginCallback");

		return mv;
	}

	// 로그아웃
	@RequestMapping(value = "/logout.do", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> logout(HttpServletRequest request, @RequestBody Map<String, Object> map) throws Exception {

		HttpSession session = request.getSession(false);
		if (session != null) session.invalidate();

		String url = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort()
				+ request.getContextPath() + "/main.do";
		map.put("URL", url);

		return map;
	}

	// 아이디 찾기 폼
	@RequestMapping(value = "/findId.do")
	public ModelAndView findId(CommandMap commandMap) throws Exception {
		ModelAndView mv = new ModelAndView("login/findId");

		return mv;
	}

	// 아이디 찾기
	@RequestMapping(value = "/findIdAction.do", method = RequestMethod.POST)
	public String selectSearchMyId(HttpSession session, CommandMap commandMap, RedirectAttributes ra) throws Exception {
		String email = (String) commandMap.get("MEMBER_EMAIL");
		Map<String, Object> map = loginService.selectFindId(commandMap.getMap());
		if (map == null) {
			ra.addFlashAttribute("resultMsg", "입력된 정보가 일치하지 않습니다.");
			return "redirect:/findId.do";
		}
		String user_name = (String) map.get("MEMBER_NAME");
		String user = (String) map.get("MEMBER_ID");

		String subject = "<JM COLLECTION>" + user_name + "님, 아이디 찾기 결과 입니다.";
		StringBuilder sb = new StringBuilder();
		sb.append("귀하의 아이디는 " + user + " 입니다.");
//		joinService.send(subject, sb.toString(), "1teampjt@gmail.com", email, null);
		ra.addFlashAttribute("resultMsg", "귀하의 아이디는 " + user + " 입니다.");
		ra.addFlashAttribute("isResult", "1");

		return "redirect:/findId.do";
	}

	// 비밀번호 초기화 폼
	@RequestMapping(value = "/findPw.do")
	public ModelAndView findPw(CommandMap commandMap) throws Exception {
		ModelAndView mv = new ModelAndView("login/findPw");

		return mv;
	}

	// 비밀번호 초기화
	@RequestMapping(value = "/findPwAction.do", method = RequestMethod.POST)
	public String sendMailPassword(HttpSession session, CommandMap commandMap, RedirectAttributes ra) throws Exception {
		String email = (String) commandMap.get("MEMBER_EMAIL");
		String user = loginService.selectFindPw(commandMap.getMap());

		if (user == null) {
			ra.addFlashAttribute("resultMsg", "입력된 정보가 일치하지 않습니다.");
			return "redirect:/findPw.do";
		}

		int ran = new Random().nextInt(100000) + 10000;
		String password = String.valueOf(ran);

		commandMap.put("MEMBER_PASSWD", password);
		loginService.updatePw(commandMap.getMap());

		String subject = "<JM COLLECTION>임시 비밀번호입니다.";
		StringBuilder sb = new StringBuilder();
		sb.append("귀하의 임시 비밀번호는 " + password + " 입니다. 로그인 후 패스워드를 변경해 주세요.");
//		joinService.send(subject, sb.toString(), "1teampjt@gmail.com", email, null);
		ra.addFlashAttribute("resultMsg", "귀하의 임시 비밀번호는 " + password + " 입니다.");
		ra.addFlashAttribute("isResult", "1");

		return "redirect:/findPw.do";
	}
}
