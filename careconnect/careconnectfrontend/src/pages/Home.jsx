import { Link } from 'react-router-dom'

const services = [
  { icon: '🍼', title: 'Baby Care', text: 'Thoughtful support for your little one, from daily routines to attentive companionship.', link: '/baby-care', color: 'peach' },
  { icon: '🌿', title: 'Elder Care', text: 'Respectful assistance and companionship to help older loved ones feel supported.', link: '/elder-care', color: 'mint' },
  { icon: '🤝', title: 'Trusted Caregivers', text: 'Explore care professionals and choose support that fits your family needs.', link: '/caregivers', color: 'lavender' }
]

export default function Home() {
  return <>
    <section className="hero">
      <div className="hero-copy">
        <div className="pill"><span className="pulse-dot"></span> CARE THAT BRINGS PEACE OF MIND</div>
        <h1>Because your loved ones deserve <span>the very best care.</span></h1>
        <p className="hero-description">Reliable baby care and elder care support, made simpler for families. Find the help you need with CareConnect.</p>
        <div className="hero-actions">
          <Link to="/caregivers" className="btn btn-primary">Find a caregiver <span>→</span></Link>
          <Link to="/register" className="btn btn-outline">Join CareConnect</Link>
        </div>
        <div className="trust-row"><div className="trust-avatars"><span>👩</span><span>👨</span><span>👵</span></div><div><strong>Care starts with trust</strong><small>Support for every generation</small></div></div>
      </div>
      <div className="hero-art">
        <div className="hero-orbit orbit-one"></div><div className="hero-orbit orbit-two"></div>
        <div className="hero-heart">♡</div>
        <div className="art-card art-card-top"><span className="art-icon">✓</span><div><strong>Care you can trust</strong><small>Family-first support</small></div></div>
        <div className="family-illustration">
          <div className="sun"></div><div className="person person-left"><div className="head"></div><div className="body"></div></div>
          <div className="person person-middle"><div className="head"></div><div className="body"></div></div>
          <div className="person person-right"><div className="head"></div><div className="body"></div></div>
          <div className="ground"></div>
        </div>
        <div className="art-card art-card-bottom"><span className="art-icon green">♡</span><div><strong>Here for your family</strong><small>Baby care • Elder care</small></div></div>
      </div>
    </section>
    <section className="stats-strip">
      <div><strong>01</strong><span>Family-first approach</span></div><div><strong>02</strong><span>Simple booking flow</span></div><div><strong>03</strong><span>One connected platform</span></div>
    </section>
    <section className="section services-section">
      <div className="section-heading"><p className="eyebrow">HOW WE HELP</p><h2>Care for every chapter of life</h2><p>One place to explore support for the people who matter most.</p></div>
      <div className="service-grid">{services.map(service => <article className="service-card" key={service.title}>
        <div className={`service-icon ${service.color}`}>{service.icon}</div><h3>{service.title}</h3><p>{service.text}</p><Link to={service.link} className="text-link">Explore service <span>→</span></Link>
      </article>)}</div>
    </section>
    <section className="cta-banner"><div><p className="eyebrow">YOUR FAMILY, OUR PRIORITY</p><h2>Let's make caring a little easier.</h2><p>Create an account to explore caregivers and manage your care requests.</p></div><Link to="/register" className="btn btn-light">Get started <span>→</span></Link></section>
  </>
}
