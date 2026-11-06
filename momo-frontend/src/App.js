import "./App.css";
import { BrowserRouter, Routes, Route } from "react-router-dom";

import Home from "./pages/Home";
import MovieDetail from "./pages/MovieDetail";
import Login from "./pages/Login";
import SignUp from "./pages/SignUp";
import Layout from "./pages/Layout";
import Profile from "./pages/Profile";
import ProfileEdit from "./pages/ProfileEdit";
import ProfileDelete from "./pages/ProfileDelete";
import Search from "./pages/Search";
import AllReviews from "./pages/AllReviews";
import ReviewDetail from "./pages/ReviewDetail";
import FavoriteMovies from "./pages/FavoriteMovies";
import PersonDetail from "./pages/PersonDetail"; // 1. PersonDetail 컴포넌트 임포트 추가

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<Layout />}>
          <Route path="/" element={<Home />} />
          <Route path="/movie/:id" element={<MovieDetail />} />
          <Route path="/search" element={<Search />} />
          <Route path="/reviews" element={<AllReviews />} />
          <Route path="/reviews/:reviewId" element={<ReviewDetail />} />

          {/* 2. 인물 상세 페이지 라우트 추가 */}
          <Route path="/person/:type/:identifier" element={<PersonDetail />} />

          <Route path="/profile" element={<Profile />} />
          <Route path="/profile/edit" element={<ProfileEdit />} />
          <Route path="/profile/delete" element={<ProfileDelete />} />
          <Route path="/profile/favorites" element={<FavoriteMovies />} />
        </Route>

        <Route path="/login" element={<Login />} />
        <Route path="/signup" element={<SignUp />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
